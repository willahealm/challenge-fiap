package br.com.fiap.embarquefacil;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import br.com.fiap.embarquefacil.databinding.ActivityMainBinding;
import br.com.fiap.embarquefacil.ui.AppViewModel;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            // The root already applies system-bar padding; prevent child navigation
            // views from adding the same bottom inset a second time.
            return new WindowInsetsCompat.Builder(windowInsets)
                    .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.NONE)
                    .build();
        });

        NavHostFragment host = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host);
        if (host == null) return;
        NavController nav = host.getNavController();
        NavigationUI.setupWithNavController(binding.bottomNavigation, nav);
        nav.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            boolean showBottom = id == R.id.homeFragment || id == R.id.profileFragment;
            binding.bottomNavigation.setVisibility(showBottom ? View.VISIBLE : View.GONE);
        });

        AppViewModel viewModel = new ViewModelProvider(this).get(AppViewModel.class);
        viewModel.messageEvent().observe(this, event -> {
            if (event == null) return;
            String message = event.consume();
            if (message != null && !message.isBlank()) Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }
}
