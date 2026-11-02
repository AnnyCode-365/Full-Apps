package com.example.controllers;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import com.example.FloatingService;
import com.example.ui.StatusManager;
import com.example.utils.Preferences;

/**
 * Controlador de ciclo de vida do Floating Service (Janela Flutuante).
 * Compatível com Java 7.
 */
public class FloatingController {

    private final Context context;
    private final Preferences preferences;

    public FloatingController(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = new Preferences(context);
    }

    public boolean canDrawOverlays() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true;
    }

    public boolean start() {
        if (!canDrawOverlays()) {
            return false;
        }
        Intent intent = new Intent(context, FloatingService.class);
        intent.setAction(FloatingService.ACTION_SHOW);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
        preferences.setFloatingEnabled(true);
        StatusManager.getInstance().setFloatingRunning(true);
        return true;
    }

    public void stop() {
        Intent intent = new Intent(context, FloatingService.class);
        intent.setAction(FloatingService.ACTION_HIDE);
        context.startService(intent);
        preferences.setFloatingEnabled(false);
        StatusManager.getInstance().setFloatingRunning(false);
    }

    public boolean isRunning() {
        return StatusManager.getInstance().isFloatingRunning();
    }

    public String getStatus() {
        return isRunning() ? "Painel Flutuante Ativo" : "Painel Flutuante Oculto";
    }
}
