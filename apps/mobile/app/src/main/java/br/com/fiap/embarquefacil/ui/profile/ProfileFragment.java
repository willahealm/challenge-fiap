package br.com.fiap.embarquefacil.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import br.com.fiap.embarquefacil.R;
import br.com.fiap.embarquefacil.data.model.AuthSession;
import br.com.fiap.embarquefacil.databinding.FragmentProfileBinding;
import br.com.fiap.embarquefacil.ui.AppViewModel;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private AppViewModel viewModel;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        viewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);
        AuthSession session = viewModel.session().getValue();
        String name = session != null && session.user != null ? session.user.name : "Passageiro";
        String email = session != null && session.user != null ? session.user.email : "";
        binding.userText.setText(name + (email == null || email.isBlank() ? "" : "\n" + email));
        binding.demoSwitch.setChecked(viewModel.settings().isDemoMode());
        binding.apiUrlInput.setText(viewModel.settings().getBaseUrl());
        binding.apiUrlInput.setEnabled(!binding.demoSwitch.isChecked());
        binding.demoSwitch.setOnCheckedChangeListener((button, checked) -> binding.apiUrlInput.setEnabled(!checked));
        binding.saveButton.setOnClickListener(v -> {
            String url = binding.apiUrlInput.getText() == null ? "" : binding.apiUrlInput.getText().toString();
            if (!binding.demoSwitch.isChecked() && !viewModel.settings().isValidBaseUrl(url)) {
                binding.apiUrlLayout.setError("Use uma URL http:// ou https:// válida.");
                return;
            }
            binding.apiUrlLayout.setError(null);
            viewModel.settings().setBaseUrl(url);
            viewModel.settings().setDemoMode(binding.demoSwitch.isChecked());
            Toast.makeText(requireContext(), "Ambiente salvo. Entre novamente para criar uma sessão válida.", Toast.LENGTH_LONG).show();
            viewModel.logoutLocal();
        });
        binding.resetDemoButton.setOnClickListener(v -> {
            viewModel.settings().setDemoMode(true);
            binding.demoSwitch.setChecked(true);
            viewModel.resetDemo();
            Toast.makeText(requireContext(), "Roteiro reiniciado: plataforma 18.", Toast.LENGTH_SHORT).show();
        });
        binding.logoutButton.setOnClickListener(v -> {
            binding.logoutButton.setEnabled(false);
            viewModel.logout();
        });
        viewModel.logoutEvent().observe(getViewLifecycleOwner(), event -> {
            if (event != null && event.consume() != null && isAdded()) {
                NavHostFragment.findNavController(this).navigate(R.id.action_profile_to_login);
            }
        });
    }

    @Override public void onDestroyView() { binding = null; super.onDestroyView(); }
}
