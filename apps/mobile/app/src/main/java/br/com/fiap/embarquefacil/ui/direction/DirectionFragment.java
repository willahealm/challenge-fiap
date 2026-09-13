package br.com.fiap.embarquefacil.ui.direction;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;

import br.com.fiap.embarquefacil.R;
import br.com.fiap.embarquefacil.data.model.JourneyDetails;
import br.com.fiap.embarquefacil.data.model.RouteStep;
import br.com.fiap.embarquefacil.databinding.FragmentDirectionBinding;
import br.com.fiap.embarquefacil.ui.AppViewModel;
import br.com.fiap.embarquefacil.util.HeadingCalculator;

public class DirectionFragment extends Fragment implements SensorEventListener {
    private FragmentDirectionBinding binding;
    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private TextToSpeech tts;
    private List<RouteStep> steps = java.util.Collections.emptyList();
    private int stepIndex;
    private int sensorAccuracy = SensorManager.SENSOR_STATUS_UNRELIABLE;
    private final ArrayDeque<Float> headingWindow = new ArrayDeque<>();
    private long startedAt;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        binding = FragmentDirectionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        AppViewModel viewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);
        JourneyDetails details = viewModel.currentJourney();
        if (details != null && details.route != null && details.route.steps != null) steps = details.route.steps;
        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        startedAt = SystemClock.elapsedRealtime();
        tts = new TextToSpeech(requireContext(), status -> {
            if (status == TextToSpeech.SUCCESS) tts.setLanguage(new Locale("pt", "BR"));
        });
        binding.speakButton.setOnClickListener(v -> speak());
        binding.recalibrateButton.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Recalibrar bússola")
                .setMessage("Afaste o celular de metais e mova-o lentamente desenhando um oito no ar. A instrução textual continua válida mesmo sem a seta.")
                .setPositiveButton("Entendi", null).show());
        binding.previousButton.setOnClickListener(v -> { if (stepIndex > 0) { stepIndex--; renderStep(); } });
        binding.nextButton.setOnClickListener(v -> { if (stepIndex + 1 < steps.size()) { stepIndex++; renderStep(); } });
        renderStep();
        if (rotationSensor == null) showFallback("Sensor de direção não disponível");
    }

    private void renderStep() {
        if (binding == null) return;
        if (steps.isEmpty()) {
            binding.destinationText.setText("Rota em etapas");
            binding.instructionText.setText("Siga as placas do terminal até a plataforma informada.");
            binding.distanceText.setText("Confirme sua posição em um QR ou totem.");
            showFallback("Rota direcional indisponível");
            binding.previousButton.setEnabled(false);
            binding.nextButton.setEnabled(false);
            return;
        }
        RouteStep step = steps.get(stepIndex);
        binding.destinationText.setText(step.displayTitle());
        binding.instructionText.setText(step.instruction);
        binding.distanceText.setText("Aproximadamente " + step.distanceMeters + " m • Etapa " + (stepIndex + 1) + " de " + steps.size());
        binding.previousButton.setEnabled(stepIndex > 0);
        binding.nextButton.setEnabled(stepIndex + 1 < steps.size());
    }

    @Override public void onResume() {
        super.onResume();
        if (rotationSensor != null) sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI);
    }

    @Override public void onPause() {
        if (sensorManager != null) sensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override public void onSensorChanged(SensorEvent event) {
        if (binding == null || steps.isEmpty()) return;
        float[] matrix = new float[9];
        float[] orientation = new float[3];
        SensorManager.getRotationMatrixFromVector(matrix, event.values);
        SensorManager.getOrientation(matrix, orientation);
        float heading = (float) Math.toDegrees(orientation[0]);
        if (heading < 0) heading += 360f;
        headingWindow.addLast(heading);
        if (headingWindow.size() > 12) headingWindow.removeFirst();
        RouteStep step = steps.get(stepIndex);
        float rotation = HeadingCalculator.arrowRotation(step.headingDegrees, heading);
        binding.arrowImage.animate().rotation(rotation).setDuration(180).start();
        updateConfidence();
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {
        sensorAccuracy = accuracy;
        updateConfidence();
    }

    private void updateConfidence() {
        if (binding == null) return;
        float spread = headingSpread();
        if (sensorAccuracy >= SensorManager.SENSOR_STATUS_ACCURACY_HIGH && spread < 18f) {
            binding.confidenceText.setText("Confiança do sensor: Boa");
            binding.confidenceText.setTextColor(requireContext().getColor(R.color.ef_success));
            binding.arrowImage.setAlpha(1f);
            binding.fallbackText.setVisibility(View.GONE);
        } else if (sensorAccuracy >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM && spread < 45f) {
            binding.confidenceText.setText("Confiança do sensor: Regular");
            binding.confidenceText.setTextColor(requireContext().getColor(R.color.ef_warning));
            binding.arrowImage.setAlpha(0.72f);
        } else {
            binding.confidenceText.setText("Confiança do sensor: Baixa");
            binding.confidenceText.setTextColor(requireContext().getColor(R.color.ef_error));
            binding.arrowImage.setAlpha(0.35f);
            if (SystemClock.elapsedRealtime() - startedAt > 4000) {
                showFallback("Sensor instável");
            }
        }
    }

    private float headingSpread() {
        if (headingWindow.size() < 3) return 99f;
        double x = 0, y = 0;
        for (float heading : headingWindow) {
            double radians = Math.toRadians(heading);
            x += Math.cos(radians);
            y += Math.sin(radians);
        }
        double mean = Math.atan2(y, x);
        double max = 0;
        for (float heading : headingWindow) {
            double delta = Math.abs(Math.atan2(Math.sin(Math.toRadians(heading) - mean), Math.cos(Math.toRadians(heading) - mean)));
            max = Math.max(max, Math.toDegrees(delta));
        }
        return (float) max;
    }

    private void showFallback(String reason) {
        if (binding == null) return;
        binding.confidenceText.setText("Confiança do sensor: Baixa");
        binding.fallbackText.setText(reason + ". Use as instruções por etapas e confirme sua posição no próximo QR ou totem.");
        binding.fallbackText.setVisibility(View.VISIBLE);
        binding.arrowImage.setAlpha(0.25f);
    }

    private void speak() {
        if (tts == null || steps.isEmpty()) return;
        RouteStep step = steps.get(stepIndex);
        tts.speak(step.instruction + " Aproximadamente " + step.distanceMeters + " metros.", TextToSpeech.QUEUE_FLUSH, null, "route-step");
    }

    @Override public void onDestroyView() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        binding = null;
        super.onDestroyView();
    }
}
