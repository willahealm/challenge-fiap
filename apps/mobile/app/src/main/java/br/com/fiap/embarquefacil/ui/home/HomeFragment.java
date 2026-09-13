package br.com.fiap.embarquefacil.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import br.com.fiap.embarquefacil.R;
import br.com.fiap.embarquefacil.data.model.AuthSession;
import br.com.fiap.embarquefacil.data.model.Trip;
import br.com.fiap.embarquefacil.databinding.FragmentHomeBinding;
import br.com.fiap.embarquefacil.ui.AppViewModel;
import br.com.fiap.embarquefacil.ui.UiState;
import br.com.fiap.embarquefacil.util.Formatters;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private AppViewModel viewModel;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        viewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);
        AuthSession session = viewModel.session().getValue();
        String name = session != null && session.user != null ? session.user.name : "passageiro";
        binding.greetingText.setText("Olá, " + name);
        binding.retryButton.setOnClickListener(v -> viewModel.loadNextTrip());
        binding.prepareButton.setOnClickListener(v -> {
            viewModel.loadJourney(true);
            NavHostFragment.findNavController(this).navigate(R.id.action_home_to_journey);
        });
        viewModel.tripState().observe(getViewLifecycleOwner(), stateValue -> render(stateValue));
        if (viewModel.tripState().getValue() == null || viewModel.tripState().getValue().data == null) {
            viewModel.loadNextTrip();
        }
    }

    private void render(UiState<Trip> state) {
        binding.progress.setVisibility(state.status == UiState.Status.LOADING && state.data == null ? View.VISIBLE : View.GONE);
        binding.errorGroup.setVisibility(state.status == UiState.Status.ERROR && state.data == null ? View.VISIBLE : View.GONE);
        if (state.status == UiState.Status.ERROR) binding.errorText.setText(state.message);
        Trip trip = state.data;
        binding.contentGroup.setVisibility(trip == null ? View.GONE : View.VISIBLE);
        if (trip == null) return;
        binding.routeText.setText(trip.origin + "  →  " + trip.destination);
        binding.dateText.setText(Formatters.departure(trip.departureAt));
        binding.carrierText.setText(trip.carrierName() + (trip.terminal == null ? "" : " • " + trip.terminal));
        binding.platformText.setText("Plataforma " + trip.platform);
        binding.statusText.setText("● " + Formatters.status(trip.status));
        boolean changed = "21".equals(trip.platform);
        binding.alertCard.setVisibility(changed ? View.VISIBLE : View.GONE);
        if (changed) binding.alertText.setText("⚠ Plataforma alterada para 21. Abra sua jornada para ver a nova rota.");
    }

    @Override public void onResume() {
        super.onResume();
        if (viewModel != null && viewModel.session().getValue() == null) {
            NavHostFragment.findNavController(this).navigate(R.id.loginFragment);
        }
    }

    @Override public void onDestroyView() { binding = null; super.onDestroyView(); }
}
