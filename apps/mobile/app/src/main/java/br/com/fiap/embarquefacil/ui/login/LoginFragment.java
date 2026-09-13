package br.com.fiap.embarquefacil.ui.login;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import br.com.fiap.embarquefacil.R;
import br.com.fiap.embarquefacil.databinding.FragmentLoginBinding;
import br.com.fiap.embarquefacil.ui.AppViewModel;
import br.com.fiap.embarquefacil.ui.UiState;

public class LoginFragment extends Fragment {
    private FragmentLoginBinding binding;
    private AppViewModel viewModel;
    private boolean navigated;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        viewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);
        binding.demoSwitch.setChecked(viewModel.settings().isDemoMode());
        binding.apiUrlInput.setText(viewModel.settings().getBaseUrl());
        binding.emailInput.setText(R.string.demo_email);
        binding.passwordInput.setText(R.string.demo_password);

        binding.loginButton.setOnClickListener(v -> submit(false));
        binding.demoButton.setOnClickListener(v -> submit(true));
        binding.demoSwitch.setOnCheckedChangeListener((button, checked) -> {
            viewModel.settings().setDemoMode(checked);
            binding.apiUrlInput.setEnabled(!checked);
        });
        binding.apiUrlInput.setEnabled(!binding.demoSwitch.isChecked());

        viewModel.loginState().observe(getViewLifecycleOwner(), ui -> {
            boolean loading = ui.status == UiState.Status.LOADING;
            binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.loginButton.setEnabled(!loading);
            binding.demoButton.setEnabled(!loading);
            if (ui.status == UiState.Status.ERROR) {
                binding.errorText.setText(ui.message);
                binding.errorText.setVisibility(View.VISIBLE);
            } else {
                binding.errorText.setVisibility(View.GONE);
            }
            if (ui.status == UiState.Status.SUCCESS) navigateHome();
        });

        if (viewModel.session().getValue() != null) navigateHome();
    }

    private void submit(boolean forceDemo) {
        String email = text(binding.emailInput.getText());
        String password = text(binding.passwordInput.getText());
        if (forceDemo) {
            email = getString(R.string.demo_email);
            password = getString(R.string.demo_password);
            binding.demoSwitch.setChecked(true);
        }
        if (TextUtils.isEmpty(email) || !email.contains("@")) {
            binding.emailLayout.setError("Informe um e-mail válido.");
            return;
        }
        binding.emailLayout.setError(null);
        if (password.length() < 6) {
            binding.passwordLayout.setError("A senha deve ter ao menos 6 caracteres.");
            return;
        }
        binding.passwordLayout.setError(null);
        String apiUrl = text(binding.apiUrlInput.getText());
        if (!binding.demoSwitch.isChecked() && !viewModel.settings().isValidBaseUrl(apiUrl)) {
            binding.apiUrlLayout.setError("Use uma URL http:// ou https:// válida.");
            return;
        }
        binding.apiUrlLayout.setError(null);
        viewModel.settings().setBaseUrl(apiUrl);
        viewModel.login(email, password);
    }

    private String text(CharSequence value) { return value == null ? "" : value.toString().trim(); }

    private void navigateHome() {
        if (navigated || !isAdded()) return;
        navigated = true;
        NavHostFragment.findNavController(this).navigate(R.id.action_login_to_home);
    }

    @Override public void onDestroyView() { binding = null; super.onDestroyView(); }
}
