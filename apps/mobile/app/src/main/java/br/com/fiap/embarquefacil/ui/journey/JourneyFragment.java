package br.com.fiap.embarquefacil.ui.journey;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

import br.com.fiap.embarquefacil.R;
import br.com.fiap.embarquefacil.data.model.JourneyDetails;
import br.com.fiap.embarquefacil.data.model.OperationalAlert;
import br.com.fiap.embarquefacil.databinding.FragmentJourneyBinding;
import br.com.fiap.embarquefacil.ui.AppViewModel;
import br.com.fiap.embarquefacil.ui.UiState;

public class JourneyFragment extends Fragment {
    private FragmentJourneyBinding binding;
    private AppViewModel viewModel;
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private boolean bindingChecklist;
    private final Runnable poll = new Runnable() {
        @Override public void run() {
            if (isResumed() && viewModel != null) viewModel.loadJourney(false);
            pollHandler.postDelayed(this, 2000);
        }
    };

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        binding = FragmentJourneyBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        viewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);
        binding.refreshButton.setOnClickListener(v -> viewModel.loadJourney(true));
        binding.scannerButton.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.action_journey_to_scanner));
        binding.directionButton.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.action_journey_to_direction));
        binding.handoffButton.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.action_journey_to_handoff));
        binding.completeButton.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Confirmar chegada")
                .setMessage("Você já está na plataforma indicada?")
                .setNegativeButton("Ainda não", null)
                .setPositiveButton("Sim, cheguei", (dialog, which) -> viewModel.completeJourney())
                .show());
        binding.checkDocument.setOnCheckedChangeListener((button, checked) -> saveChecklist());
        binding.checkTicket.setOnCheckedChangeListener((button, checked) -> saveChecklist());
        binding.checkLuggage.setOnCheckedChangeListener((button, checked) -> saveChecklist());
        binding.checkTime.setOnCheckedChangeListener((button, checked) -> saveChecklist());

        viewModel.journeyState().observe(getViewLifecycleOwner(), this::render);
        viewModel.alertEvent().observe(getViewLifecycleOwner(), event -> {
            if (event == null) return;
            OperationalAlert alert = event.consume();
            if (alert != null) showAlert(alert);
        });
        viewModel.completedEvent().observe(getViewLifecycleOwner(), event -> {
            if (event != null && event.consume() != null && isAdded()) {
                NavHostFragment.findNavController(this).navigate(R.id.action_journey_to_feedback);
            }
        });
        if (viewModel.currentJourney() == null) viewModel.loadJourney(true);
    }

    @Override public void onResume() {
        super.onResume();
        pollHandler.removeCallbacks(poll);
        pollHandler.postDelayed(poll, 2000);
    }

    @Override public void onPause() {
        pollHandler.removeCallbacks(poll);
        super.onPause();
    }

    private void render(UiState<JourneyDetails> state) {
        boolean firstLoad = state.status == UiState.Status.LOADING && state.data == null;
        binding.progress.setVisibility(firstLoad ? View.VISIBLE : View.GONE);
        binding.errorText.setVisibility(state.status == UiState.Status.ERROR ? View.VISIBLE : View.GONE);
        if (state.status == UiState.Status.ERROR) binding.errorText.setText(state.message + " Você pode tentar Atualizar.");
        JourneyDetails details = state.data;
        binding.contentGroup.setVisibility(details == null ? View.GONE : View.VISIBLE);
        if (details == null || details.trip == null || details.journey == null) return;

        binding.routeText.setText(details.trip.origin + "  →  " + details.trip.destination);
        binding.platformText.setText("Plataforma atual: " + details.trip.platform);
        String point = details.journey.currentPointName != null ? details.journey.currentPointName : details.journey.currentPointId;
        binding.locationText.setText("Local confirmado: " + (point == null ? "ainda não" : point));
        binding.timelineText.setText(timeline(details.journey.stage));

        bindingChecklist = true;
        Map<String, Boolean> checks = details.journey.checklist == null ? java.util.Collections.emptyMap() : details.journey.checklist;
        binding.checkDocument.setChecked(Boolean.TRUE.equals(checks.get("document")));
        binding.checkTicket.setChecked(Boolean.TRUE.equals(checks.get("ticket")));
        binding.checkLuggage.setChecked(Boolean.TRUE.equals(checks.get("luggage")));
        binding.checkTime.setChecked(Boolean.TRUE.equals(checks.get("departureTime")));
        bindingChecklist = false;

        OperationalAlert latest = details.latestAlert();
        binding.alertCard.setVisibility(latest == null ? View.GONE : View.VISIBLE);
        if (latest != null) binding.alertText.setText("⚠ " + latest.message);
    }

    private String timeline(String stage) {
        if ("COMPLETED".equals(stage)) return "✓ Preparar\n✓ Sair de casa\n✓ Chegar ao terminal\n✓ Embarcar";
        if ("AT_TERMINAL".equals(stage)) return "✓ Preparar\n✓ Sair de casa\n● Chegar ao terminal\n○ Embarcar";
        return "● Preparar\n○ Sair de casa\n○ Chegar ao terminal\n○ Embarcar";
    }

    private void saveChecklist() {
        if (bindingChecklist || binding == null) return;
        Map<String, Boolean> values = new LinkedHashMap<>();
        values.put("document", binding.checkDocument.isChecked());
        values.put("ticket", binding.checkTicket.isChecked());
        values.put("luggage", binding.checkLuggage.isChecked());
        values.put("departureTime", binding.checkTime.isChecked());
        viewModel.updateChecklist(values);
    }

    private void showAlert(OperationalAlert alert) {
        new MaterialAlertDialogBuilder(requireContext())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Plataforma alterada")
                .setMessage(alert.message + "\n\nNova plataforma: " + alert.platform)
                .setPositiveButton("Entendi", null)
                .show();
    }

    @Override public void onDestroyView() {
        pollHandler.removeCallbacks(poll);
        binding = null;
        super.onDestroyView();
    }
}
