package com.example.mesto_samostojna;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Poceten (splash) ekran so brendot "Mesto".
 *
 * ZOSO: kratok branding pred glavniot ekran.
 * KAKO: tap ili ~2.4 s → MainActivity; flag "navigated" sprecuva dvojen otvor.
 */
public class SplashActivity extends AppCompatActivity {

    // Avtomatski premin ako korisnikot ne tapne porano.
    private static final long AUTO_NAVIGATE_MS = 2400L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean navigated;

    private final Runnable navigateRunnable =
            () -> {
                if (!navigated) {
                    goToMain();
                }
            };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View root = findViewById(R.id.splash_root);
        root.setOnClickListener(v -> goToMain());

        handler.postDelayed(navigateRunnable, AUTO_NAVIGATE_MS);
    }

    @Override
    protected void onDestroy() {
        // Otstrani go zakazaniot premin — inaku Handler-ot moze da drzi
        // referenca kon unistena Activity (memory leak / crash).
        handler.removeCallbacks(navigateRunnable);
        super.onDestroy();
    }

    /** Odi na MainActivity samo ednas (navigated flag = zastita od dvoen otvor). */
    private void goToMain() {
        if (navigated) {
            return;
        }
        navigated = true;
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
