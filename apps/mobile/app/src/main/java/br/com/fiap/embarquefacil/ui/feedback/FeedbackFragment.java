package br.com.fiap.embarquefacil.ui.feedback;

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
import br.com.fiap.embarquefacil.databinding.FragmentFeedbackBinding;
import br.com.fiap.embarquefacil.ui.AppViewModel;

public class FeedbackFragment extends Fragment {
    private FragmentFeedbackBinding binding;
    private AppViewModel viewModel;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        binding = FragmentFeedbackBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        viewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);
        binding.submitButton.setOnClickListener(v -> submit());
        binding.skipButton.setOnClickListener(v -> finish());
        viewModel.feedbackEvent().observe(getViewLifecycleOwner(), event -> {
            if (event != null && event.consume() != null) finish();
        });
    }

    private void submit() {
        int rating = Math.round(binding.ratingBar.getRating());
        if (rating < 1) {
            binding.errorText.setText("Escolha uma nota de 1 a 5.");
            binding.errorText.setVisibility(View.VISIBLE);
            return;
        }
        binding.errorText.setVisibility(View.GONE);
        binding.submitButton.setEnabled(false);
        String tag = "";
        int checked = binding.tagGroup.getCheckedChipId();
        if (checked == R.id.tag_orientation) tag = "ORIENTATION";
        else if (checked == R.id.tag_information) tag = "INFORMATION";
        else if (checked == R.id.tag_accessibility) tag = "ACCESSIBILITY";
        String comment = binding.commentInput.getText() == null ? "" : binding.commentInput.getText().toString().trim();
        viewModel.submitFeedback(rating, tag, comment);
    }

    private void finish() {
        if (!isAdded()) return;
        NavHostFragment.findNavController(this).navigate(R.id.action_feedback_to_home);
    }

    @Override public void onDestroyView() { binding = null; super.onDestroyView(); }
}
