package br.com.fiap.embarquefacil.ui.scanner;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import br.com.fiap.embarquefacil.databinding.FragmentScannerBinding;
import br.com.fiap.embarquefacil.ui.AppViewModel;

@OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
public class ScannerFragment extends Fragment {
    private FragmentScannerBinding binding;
    private AppViewModel viewModel;
    private ExecutorService cameraExecutor;
    private BarcodeScanner scanner;
    private ProcessCameraProvider cameraProvider;
    private final AtomicBoolean submitting = new AtomicBoolean(false);
    private final ActivityResultLauncher<String> cameraPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startCamera(); else showCameraFallback("Câmera não autorizada. Digite o código da placa.");
            });

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        binding = FragmentScannerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        viewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);
        cameraExecutor = Executors.newSingleThreadExecutor();
        scanner = BarcodeScanning.getClient();
        binding.cameraButton.setOnClickListener(v -> requestCamera());
        binding.confirmButton.setOnClickListener(v -> submit(text(binding.codeInput.getText())));
        viewModel.checkpointEvent().observe(getViewLifecycleOwner(), event -> {
            if (event == null) return;
            Boolean success = event.consume();
            if (success == null) return;
            if (success) NavHostFragment.findNavController(this).navigateUp();
            else {
                submitting.set(false);
                binding.confirmButton.setEnabled(true);
            }
        });
        requestCamera();
    }

    private void requestCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        if (binding == null) return;
        binding.cameraMessageGroup.setVisibility(View.GONE);
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(requireContext());
        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.preview.getSurfaceProvider());
                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                analysis.setAnalyzer(cameraExecutor, this::analyze);
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(getViewLifecycleOwner(), CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis);
            } catch (Exception error) {
                showCameraFallback("Câmera indisponível neste aparelho. Use o código manual.");
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void analyze(ImageProxy proxy) {
        if (proxy.getImage() == null || submitting.get()) {
            proxy.close();
            return;
        }
        InputImage image = InputImage.fromMediaImage(proxy.getImage(), proxy.getImageInfo().getRotationDegrees());
        scanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    for (Barcode barcode : barcodes) {
                        String value = barcode.getRawValue();
                        if (value != null && !value.isBlank()) {
                            submit(value);
                            break;
                        }
                    }
                })
                .addOnCompleteListener(task -> proxy.close());
    }

    private void submit(String code) {
        if (code.isBlank()) {
            binding.codeInput.setError("Digite o código encontrado na placa.");
            return;
        }
        if (!submitting.compareAndSet(false, true)) return;
        binding.confirmButton.setEnabled(false);
        if (cameraProvider != null) cameraProvider.unbindAll();
        viewModel.validateCheckpoint(code);
    }

    private void showCameraFallback(String message) {
        if (binding == null) return;
        binding.cameraMessage.setText(message);
        binding.cameraMessageGroup.setVisibility(View.VISIBLE);
    }

    private String text(CharSequence value) { return value == null ? "" : value.toString().trim(); }

    @Override public void onDestroyView() {
        if (cameraProvider != null) cameraProvider.unbindAll();
        if (scanner != null) scanner.close();
        if (cameraExecutor != null) cameraExecutor.shutdown();
        binding = null;
        super.onDestroyView();
    }
}
