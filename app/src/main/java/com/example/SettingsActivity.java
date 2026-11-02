package com.example;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.example.controllers.NetworkController;
import com.example.ui.FloatingPanel;
import com.example.utils.Preferences;

/**
 * Atividade de Ajustes do GHOST NETWORK.
 * Configuração de opacidade, parâmetros de MTU, DNS, alvo de jitter e unidades.
 * Compatível com Java 7.
 */
public class SettingsActivity extends Activity {

    private Preferences preferences;
    private FloatingPanel floatingPanel;

    private TextView txtOpacityValue;
    private SeekBar seekOpacity;
    private Button btnResetPos;
    private EditText editJitterTarget;
    private EditText editJitterInterval;
    private EditText editVpnMtu;
    private EditText editVpnDns;
    private Switch switchUnitMb;
    private Button btnSave;
    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        preferences = new Preferences(this);
        floatingPanel = new FloatingPanel(this);

        txtOpacityValue = (TextView) findViewById(R.id.txt_opacity_value);
        seekOpacity = (SeekBar) findViewById(R.id.seek_opacity);
        btnResetPos = (Button) findViewById(R.id.btn_reset_window_pos);
        editJitterTarget = (EditText) findViewById(R.id.edit_jitter_target);
        editJitterInterval = (EditText) findViewById(R.id.edit_jitter_interval);
        editVpnMtu = (EditText) findViewById(R.id.edit_vpn_mtu);
        editVpnDns = (EditText) findViewById(R.id.edit_vpn_dns);
        switchUnitMb = (Switch) findViewById(R.id.switch_unit_mb);
        btnSave = (Button) findViewById(R.id.btn_save_all_settings);
        btnBack = (Button) findViewById(R.id.btn_settings_back);

        loadCurrentValues();
        setupListeners();
    }

    private void loadCurrentValues() {
        // Opacidade de 0.3 (30%) a 1.0 (100%)
        float currentAlpha = preferences.getWindowAlpha();
        int progress = (int) ((currentAlpha - 0.3f) / 0.7f * 100f);
        seekOpacity.setProgress(Math.max(0, Math.min(100, progress)));
        txtOpacityValue.setText((int) (currentAlpha * 100) + "%");

        editJitterTarget.setText(preferences.getJitterTarget());
        editJitterInterval.setText(String.valueOf(preferences.getJitterIntervalMs()));
        editVpnMtu.setText(String.valueOf(preferences.getVpnMtu()));
        editVpnDns.setText(preferences.getVpnDns());
        switchUnitMb.setChecked(preferences.isUnitMb());
    }

    private void setupListeners() {
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        seekOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float alpha = 0.3f + (progress / 100f * 0.7f);
                txtOpacityValue.setText((int) (alpha * 100) + "%");
                floatingPanel.setOpacity(alpha);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnResetPos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                floatingPanel.resetPosition();
                Toast.makeText(SettingsActivity.this, "Posição da janela redefinida!", Toast.LENGTH_SHORT).show();
            }
        });

        switchUnitMb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                preferences.setUnitMb(isChecked);
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAllSettings();
            }
        });
    }

    private void saveAllSettings() {
        String target = editJitterTarget.getText().toString().trim();
        String intervalStr = editJitterInterval.getText().toString().trim();
        String mtuStr = editVpnMtu.getText().toString().trim();
        String dns = editVpnDns.getText().toString().trim();

        if (!target.isEmpty()) {
            preferences.setJitterTarget(target);
            NetworkController.getInstance(this).getJitterMonitor().setTargetHost(target);
        }

        try {
            long interval = Long.parseLong(intervalStr);
            if (interval >= 500) {
                preferences.setJitterIntervalMs(interval);
                NetworkController.getInstance(this).getJitterMonitor().setIntervalMs(interval);
            }
        } catch (Exception ignored) {}

        try {
            int mtu = Integer.parseInt(mtuStr);
            if (mtu >= 1280 && mtu <= 1500) {
                preferences.setVpnMtu(mtu);
            }
        } catch (Exception ignored) {}

        if (!dns.isEmpty()) {
            preferences.setVpnDns(dns);
        }

        Toast.makeText(this, "Ajustes salvos com sucesso!", Toast.LENGTH_SHORT).show();
        finish();
    }
}
