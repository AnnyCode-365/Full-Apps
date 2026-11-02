package com.example;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.example.controllers.FloatingController;
import com.example.controllers.NetworkController;
import com.example.controllers.UdpController;
import com.example.controllers.VpnController;
import com.example.ui.Dashboard;
import com.example.utils.Preferences;

/**
 * Atividade principal do GHOST NETWORK.
 * Painel completo de monitoramento, controle de VPN, Jitter, UDP e Floating Service.
 * Compatível com Java 7 e compiladores AIDE.
 */
public class MainActivity extends Activity {

    private static final int REQUEST_VPN = 1001;
    private static final int REQUEST_OVERLAY = 1002;

    private Preferences preferences;
    private FloatingController floatingController;
    private VpnController vpnController;
    private UdpController udpController;
    private NetworkController networkController;
    private Dashboard dashboard;

    private EditText editServerHost;
    private EditText editServerPort;
    private EditText editServerKey;
    private Button btnSaveConfig;
    private Button btnSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = new Preferences(this);
        floatingController = new FloatingController(this);
        vpnController = new VpnController(this);
        udpController = new UdpController(this);
        networkController = NetworkController.getInstance(this);

        dashboard = new Dashboard(
                this,
                floatingController,
                vpnController,
                udpController,
                networkController
        );

        dashboard.setVpnListener(new Dashboard.VpnPrepareRequestListener() {
            @Override
            public void onRequestVpnPermission() {
                requestVpnStart();
            }
        });

        dashboard.setOverlayListener(new Dashboard.OverlayPermissionRequestListener() {
            @Override
            public void onRequestOverlayPermission() {
                requestOverlayPermission();
            }
        });

        initServerConfigViews();
        requestNotificationPermissionIfNeeded();
    }

    private void initServerConfigViews() {
        editServerHost = (EditText) findViewById(R.id.edit_server_host);
        editServerPort = (EditText) findViewById(R.id.edit_server_port);
        editServerKey = (EditText) findViewById(R.id.edit_server_key);
        btnSaveConfig = (Button) findViewById(R.id.btn_save_vpn_config);
        btnSettings = (Button) findViewById(R.id.btn_settings);

        editServerHost.setText(preferences.getVpnServerHost());
        editServerPort.setText(String.valueOf(preferences.getVpnServerPort()));
        editServerKey.setText(preferences.getVpnSecretKey());

        btnSaveConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String host = editServerHost.getText().toString().trim();
                String portStr = editServerPort.getText().toString().trim();
                String key = editServerKey.getText().toString().trim();

                int port = 1194;
                try {
                    if (!portStr.isEmpty()) {
                        port = Integer.parseInt(portStr);
                    }
                } catch (Exception ignored) {}

                preferences.setVpnServerHost(host);
                preferences.setVpnServerPort(port);
                preferences.setVpnSecretKey(key);

                Toast.makeText(MainActivity.this, "Configurações do servidor salvas com sucesso!", Toast.LENGTH_SHORT).show();

                // Se a VPN estiver rodando, reinicia para aplicar novo túnel
                if (vpnController.isRunning()) {
                    vpnController.stop();
                    requestVpnStart();
                }
            }
        });

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });
    }

    private void requestVpnStart() {
        Intent prepareIntent = vpnController.prepareVpn();
        if (prepareIntent != null) {
            startActivityForResult(prepareIntent, REQUEST_VPN);
        } else {
            onActivityResult(REQUEST_VPN, RESULT_OK, null);
        }
    }

    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
            );
            startActivityForResult(intent, REQUEST_OVERLAY);
        } else {
            floatingController.start();
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1003);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_VPN) {
            if (resultCode == RESULT_OK) {
                vpnController.start();
                Toast.makeText(this, "VPN Local Iniciada", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permissão para VPN não concedida", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_OVERLAY) {
            if (floatingController.canDrawOverlays()) {
                floatingController.start();
                Toast.makeText(this, "Painel Flutuante Iniciado", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permissão de sobreposição necessária para o Floating Service", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dashboard != null) {
            dashboard.startListening();
        }
        // Atualiza campos caso tenham sido editados nos ajustes
        if (editServerHost != null) {
            editServerHost.setText(preferences.getVpnServerHost());
            editServerPort.setText(String.valueOf(preferences.getVpnServerPort()));
            editServerKey.setText(preferences.getVpnSecretKey());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (dashboard != null) {
            dashboard.stopListening();
        }
    }
}
