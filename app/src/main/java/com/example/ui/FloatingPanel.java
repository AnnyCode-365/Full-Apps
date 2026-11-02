package com.example.ui;

import android.content.Context;
import com.example.utils.Preferences;

/**
 * Utilitário de customização, animações e transparência para o painel flutuante.
 * Compatível com Java 7.
 */
public class FloatingPanel {

    private final Context context;
    private final Preferences preferences;

    public FloatingPanel(Context context) {
        this.context = context;
        this.preferences = new Preferences(context);
    }

    public void setOpacity(float alpha) {
        float bounded = Math.max(0.3f, Math.min(1.0f, alpha));
        preferences.setWindowAlpha(bounded);
    }

    public float getOpacity() {
        return preferences.getWindowAlpha();
    }

    public void resetPosition() {
        preferences.setWindowX(100);
        preferences.setWindowY(200);
    }
}
