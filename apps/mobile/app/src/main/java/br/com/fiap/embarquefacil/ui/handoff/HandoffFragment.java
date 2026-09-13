package br.com.fiap.embarquefacil.ui.handoff;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.time.Duration;
import java.time.Instant;

import br.com.fiap.embarquefacil.data.model.Handoff;
import br.com.fiap.embarquefacil.databinding.FragmentHandoffBinding;
import br.com.fiap.embarquefacil.ui.AppViewModel;
import br.com.fiap.embarquefacil.ui.UiState;

public class HandoffFragment extends Fragment {
    private FragmentHandoffBinding binding;
    private AppViewModel viewModel;
    private CountDownTimer timer;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        binding = FragmentHandoffBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        viewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);
        binding.regenerateButton.setOnClickListener(v -> viewModel.createHandoff());
        viewModel.handoffState().observe(getViewLifecycleOwner(), this::render);
        viewModel.createHandoff();
    }

    private void render(UiState<Handoff> state) {
        binding.progress.setVisibility(state.status == UiState.Status.LOADING ? View.VISIBLE : View.GONE);
        binding.errorText.setVisibility(state.status == UiState.Status.ERROR ? View.VISIBLE : View.GONE);
        if (state.status == UiState.Status.ERROR) binding.errorText.setText(state.message);
        binding.regenerateButton.setVisibility(state.status == UiState.Status.ERROR ? View.VISIBLE : View.GONE);
        Handoff handoff = state.data;
        if (handoff == null || handoff.token == null) return;
        try {
            binding.qrImage.setImageBitmap(qrBitmap(handoff.token, 700));
            binding.qrImage.setVisibility(View.VISIBLE);
            binding.codeText.setText(handoff.displayCode());
            binding.codeText.setVisibility(View.VISIBLE);
            startCountdown(handoff.expiresAt);
        } catch (Exception error) {
            binding.errorText.setText("Não foi possível desenhar o QR. Use o código " + handoff.displayCode() + ".");
            binding.errorText.setVisibility(View.VISIBLE);
            binding.codeText.setText(handoff.displayCode());
            binding.codeText.setVisibility(View.VISIBLE);
        }
    }

    private Bitmap qrBitmap(String text, int size) throws Exception {
        BitMatrix matrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
        }
        return bitmap;
    }

    private void startCountdown(String expiresAt) {
        if (timer != null) timer.cancel();
        long remaining = 60_000L;
        try { remaining = Math.max(0, Duration.between(Instant.now(), Instant.parse(expiresAt)).toMillis()); }
        catch (Exception ignored) { }
        timer = new CountDownTimer(remaining, 1000) {
            @Override public void onTick(long millis) {
                if (binding != null) binding.countdownText.setText("Expira em " + Math.max(1, millis / 1000) + " s");
            }
            @Override public void onFinish() {
                if (binding == null) return;
                binding.countdownText.setText("Código expirado");
                binding.qrImage.setAlpha(0.25f);
                binding.regenerateButton.setVisibility(View.VISIBLE);
            }
        }.start();
    }

    @Override public void onDestroyView() {
        if (timer != null) timer.cancel();
        binding = null;
        super.onDestroyView();
    }
}
