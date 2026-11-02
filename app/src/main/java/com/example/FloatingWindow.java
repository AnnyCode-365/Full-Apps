package com.example;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import com.example.controllers.NetworkController;
import com.example.controllers.UdpController;
import com.example.controllers.VpnController;
import com.example.ui.StatusManager;
import com.example.utils.NetworkUtils;
import com.example.utils.Preferences;

/**
 * Janela flutuante sobreposta (Overlay Window) gerenciada via WindowManager.
 * Permite movimentação por arraste, alternância entre modo compacto e expandido,
 * e controles independentes de rede.
 * Compatível com Java 7.
 */
public class FloatingWindow implements StatusManager.StatusListener {

    private final Context context;
    private final WindowManager windowManager;
    private final Preferences preferences;
    private final VpnController vpnController;
    private final UdpController udpController;

    private View rootView;
    private View layoutCompact;
    private View layoutExpanded;
    private WindowManager.LayoutParams params;

    private TextView txtCompactPing;
    private TextView txtCompactJitter;
    private TextView txtPanelPing;
    private TextView txtPanelJitter;
    private TextView txtPanelDownload;
    private TextView txtPanelUpload;

    private Switch switchFloating;
    private Switch switchVpn;
    private Switch switchJitter;
    private Switch switchUdp;

    private boolean isAdded = false;
    private boolean isMinimized = false;

    public FloatingWindow(Context context) {
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.preferences = new Preferences(context);
        this.vpnController = new VpnController(context);
        this.udpController = new UdpController(context);
        initView();
    }

    private void initView() {
        LayoutInflater inflater = LayoutInflater.from(context);
        rootView = inflater.inflate(R.layout.layout_floating_window, null);

        layoutCompact = rootView.findViewById(R.id.layout_compact);
        layoutExpanded = rootView.findViewById(R.id.layout_expanded);

        txtCompactPing = (TextView) rootView.findViewById(R.id.txt_compact_ping);
        txtCompactJitter = (TextView) rootView.findViewById(R.id.txt_compact_jitter);
        txtPanelPing = (TextView) rootView.findViewById(R.id.txt_panel_ping);
        txtPanelJitter = (TextView) rootView.findViewById(R.id.txt_panel_jitter);
        txtPanelDownload = (TextView) rootView.findViewById(R.id.txt_panel_download);
        txtPanelUpload = (TextView) rootView.findViewById(R.id.txt_panel_upload);

        switchFloating = (Switch) rootView.findViewById(R.id.switch_floating);
        switchVpn = (Switch) rootView.findViewById(R.id.switch_vpn);
        switchJitter = (Switch) rootView.findViewById(R.id.switch_jitter);
        switchUdp = (Switch) rootView.findViewById(R.id.switch_udp);

        View btnMinimize = rootView.findViewById(R.id.btn_minimize);
        View btnClose = rootView.findViewById(R.id.btn_close);
        Button btnOpenApp = (Button) rootView.findViewById(R.id.btn_open_app);

        // Minimizar / Expandir
        btnMinimize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMinimized(true);
            }
        });

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, FloatingService.class);
                intent.setAction(FloatingService.ACTION_HIDE);
                context.startService(intent);
            }
        });

        layoutCompact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMinimized(false);
            }
        });

        btnOpenApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                context.startActivity(intent);
            }
        });

        setupSwitches();
        setupDragTouch();

        isMinimized = preferences.isWindowMinimized();
        updateDisplayMode();
    }

    private void setupSwitches() {
        switchFloating.setChecked(true);
        switchFloating.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    Intent intent = new Intent(context, FloatingService.class);
                    intent.setAction(FloatingService.ACTION_HIDE);
                    context.startService(intent);
                }
            }
        });

        switchVpn.setChecked(vpnController.isRunning());
        switchVpn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    vpnController.start();
                } else {
                    vpnController.stop();
                }
            }
        });

        switchJitter.setChecked(preferences.isJitterEnabled());
        switchJitter.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                NetworkController.getInstance(context).setJitterEnabled(isChecked);
            }
        });

        switchUdp.setChecked(preferences.isUdpEnabled());
        switchUdp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    udpController.start();
                } else {
                    udpController.stop();
                }
            }
        });
    }

    private void setupDragTouch() {
        View.OnTouchListener touchListener = new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private boolean isMoving = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isMoving = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float deltaX = event.getRawX() - initialTouchX;
                        float deltaY = event.getRawY() - initialTouchY;
                        if (Math.abs(deltaX) > 8 || Math.abs(deltaY) > 8) {
                            isMoving = true;
                        }
                        params.x = initialX + (int) deltaX;
                        params.y = initialY + (int) deltaY;
                        if (isAdded && windowManager != null) {
                            windowManager.updateViewLayout(rootView, params);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (!isMoving && v == layoutCompact) {
                            setMinimized(false);
                        } else {
                            preferences.setWindowX(params.x);
                            preferences.setWindowY(params.y);
                        }
                        return true;
                }
                return false;
            }
        };

        // Arraste pelo cabeçalho no modo expandido
        rootView.findViewById(R.id.header_drag_area).setOnTouchListener(touchListener);
        // Arraste pelo botão inteiro no modo compacto
        layoutCompact.setOnTouchListener(touchListener);
    }

    public synchronized void show() {
        if (isAdded) return;

        int windowType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            windowType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            windowType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                windowType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = preferences.getWindowX();
        params.y = preferences.getWindowY();
        params.alpha = preferences.getWindowAlpha();

        try {
            windowManager.addView(rootView, params);
            isAdded = true;
            StatusManager.getInstance().addListener(this);
            StatusManager.getInstance().setFloatingRunning(true);
        } catch (Exception ignored) {}
    }

    public synchronized void hide() {
        if (!isAdded) return;
        try {
            StatusManager.getInstance().removeListener(this);
            windowManager.removeView(rootView);
            isAdded = false;
            StatusManager.getInstance().setFloatingRunning(false);
        } catch (Exception ignored) {}
    }

    public void setMinimized(boolean minimized) {
        this.isMinimized = minimized;
        preferences.setWindowMinimized(minimized);
        updateDisplayMode();
    }

    private void updateDisplayMode() {
        if (isMinimized) {
            layoutExpanded.setVisibility(View.GONE);
            layoutCompact.setVisibility(View.VISIBLE);
        } else {
            layoutCompact.setVisibility(View.GONE);
            layoutExpanded.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onStatusUpdated(final StatusManager sm) {
        if (!isAdded || rootView == null) return;

        // Atualização dos textos com valores reais
        String pingStr = NetworkUtils.formatLatency(sm.getPingMs());
        String jitterStr = NetworkUtils.formatJitter(sm.getJitterMs());
        String dlStr = NetworkUtils.formatSpeed(sm.getDownloadSpeedBps(), preferences.isUnitMb());
        String ulStr = NetworkUtils.formatSpeed(sm.getUploadSpeedBps(), preferences.isUnitMb());

        txtCompactPing.setText(pingStr);
        txtCompactJitter.setText("±" + jitterStr);

        txtPanelPing.setText(pingStr);
        txtPanelJitter.setText(jitterStr);
        txtPanelDownload.setText(dlStr);
        txtPanelUpload.setText(ulStr);

        // Atualiza switches sem disparar eventos circulares
        if (switchVpn.isChecked() != sm.isVpnRunning()) {
            switchVpn.setChecked(sm.isVpnRunning());
        }
        if (switchJitter.isChecked() != sm.isJitterRunning()) {
            switchJitter.setChecked(sm.isJitterRunning());
        }
        if (switchUdp.isChecked() != sm.isUdpRunning()) {
            switchUdp.setChecked(sm.isUdpRunning());
        }
    }
}
