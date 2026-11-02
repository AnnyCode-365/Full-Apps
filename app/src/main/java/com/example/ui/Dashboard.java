package com.example.ui;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import com.example.R;
import com.example.controllers.FloatingController;
import com.example.controllers.NetworkController;
import com.example.controllers.UdpController;
import com.example.controllers.VpnController;
import com.example.utils.NetworkUtils;
import com.example.utils.Preferences;

/**
 * Vinculador e controlador do Dashboard principal da MainActivity.
 * Compatível com Java 7.
 */
public class Dashboard implements StatusManager.StatusListener {

    private final Activity activity;
    private final Preferences preferences;
    private final FloatingController floatingController;
    private final VpnController vpnController;
    private final UdpController udpController;
    private final NetworkController networkController;

    private TextView txtVpnBadge;
    private TextView txtVpnDetails;

    private Switch switchFloating;
    private Switch switchVpn;
    private Switch switchJitter;
    private Switch switchUdp;
    private Switch switchDl;
    private Switch switchUl;

    private TextView txtPing;
    private TextView txtJitter;
    private TextView txtLoss;
    private TextView txtMinMaxAvg;
    private TextView txtJitterAlert;

    private TextView txtDlSpeed;
    private TextView txtDlTotal;
    private TextView txtUlSpeed;
    private TextView txtUlTotal;

    private TextView txtUdpPackets;
    private TextView txtUdpBytes;
    private TextView txtUdpFlows;

    private NetworkGraph graphLatency;
    private NetworkGraph graphBandwidth;

    public interface VpnPrepareRequestListener {
        void onRequestVpnPermission();
    }

    public interface OverlayPermissionRequestListener {
        void onRequestOverlayPermission();
    }

    private VpnPrepareRequestListener vpnListener;
    private OverlayPermissionRequestListener overlayListener;

    public Dashboard(Activity activity,
                     FloatingController floatingController,
                     VpnController vpnController,
                     UdpController udpController,
                     NetworkController networkController) {
        this.activity = activity;
        this.preferences = new Preferences(activity);
        this.floatingController = floatingController;
        this.vpnController = vpnController;
        this.udpController = udpController;
        this.networkController = networkController;
        bindViews();
    }

    public void setVpnListener(VpnPrepareRequestListener listener) {
        this.vpnListener = listener;
    }

    public void setOverlayListener(OverlayPermissionRequestListener listener) {
        this.overlayListener = listener;
    }

    private void bindViews() {
        txtVpnBadge = (TextView) activity.findViewById(R.id.txt_vpn_badge);
        txtVpnDetails = (TextView) activity.findViewById(R.id.txt_vpn_details);

        switchFloating = (Switch) activity.findViewById(R.id.switch_main_floating);
        switchVpn = (Switch) activity.findViewById(R.id.switch_main_vpn);
        switchJitter = (Switch) activity.findViewById(R.id.switch_main_jitter);
        switchUdp = (Switch) activity.findViewById(R.id.switch_main_udp);
        switchDl = (Switch) activity.findViewById(R.id.switch_main_dl);
        switchUl = (Switch) activity.findViewById(R.id.switch_main_ul);

        txtPing = (TextView) activity.findViewById(R.id.txt_main_ping);
        txtJitter = (TextView) activity.findViewById(R.id.txt_main_jitter);
        txtLoss = (TextView) activity.findViewById(R.id.txt_main_loss);
        txtMinMaxAvg = (TextView) activity.findViewById(R.id.txt_main_min_max_avg);
        txtJitterAlert = (TextView) activity.findViewById(R.id.txt_jitter_alert);

        txtDlSpeed = (TextView) activity.findViewById(R.id.txt_main_download_speed);
        txtDlTotal = (TextView) activity.findViewById(R.id.txt_main_download_total);
        txtUlSpeed = (TextView) activity.findViewById(R.id.txt_main_upload_speed);
        txtUlTotal = (TextView) activity.findViewById(R.id.txt_main_upload_total);

        txtUdpPackets = (TextView) activity.findViewById(R.id.txt_main_udp_packets);
        txtUdpBytes = (TextView) activity.findViewById(R.id.txt_main_udp_bytes);
        txtUdpFlows = (TextView) activity.findViewById(R.id.txt_main_udp_flows);

        graphLatency = (NetworkGraph) activity.findViewById(R.id.graph_latency);
        if (graphLatency != null) {
            graphLatency.setMode(NetworkGraph.Mode.LATENCY);
        }

        graphBandwidth = (NetworkGraph) activity.findViewById(R.id.graph_bandwidth);
        if (graphBandwidth != null) {
            graphBandwidth.setMode(NetworkGraph.Mode.BANDWIDTH);
        }

        setupControlListeners();
    }

    private void setupControlListeners() {
        // Floating Switch
        switchFloating.setChecked(floatingController.isRunning());
        switchFloating.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!floatingController.canDrawOverlays() && overlayListener != null) {
                        switchFloating.setChecked(false);
                        overlayListener.onRequestOverlayPermission();
                    } else {
                        floatingController.start();
                    }
                } else {
                    floatingController.stop();
                }
            }
        });

        // VPN Switch
        switchVpn.setChecked(vpnController.isRunning());
        switchVpn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (vpnListener != null) {
                        vpnListener.onRequestVpnPermission();
                    }
                } else {
                    vpnController.stop();
                }
            }
        });

        // Jitter Switch
        switchJitter.setChecked(preferences.isJitterEnabled());
        switchJitter.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                networkController.setJitterEnabled(isChecked);
            }
        });

        // UDP Switch
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

        // Download Monitor Switch
        switchDl.setChecked(preferences.isDownloadEnabled());
        switchDl.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                networkController.setDownloadEnabled(isChecked);
            }
        });

        // Upload Monitor Switch
        switchUl.setChecked(preferences.isUploadEnabled());
        switchUl.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                networkController.setUploadEnabled(isChecked);
            }
        });
    }

    public void startListening() {
        StatusManager.getInstance().addListener(this);
    }

    public void stopListening() {
        StatusManager.getInstance().removeListener(this);
    }

    @Override
    public void onStatusUpdated(StatusManager sm) {
        if (activity.isFinishing()) return;

        // Atualização de Switches
        if (switchFloating != null && switchFloating.isChecked() != sm.isFloatingRunning()) {
            switchFloating.setChecked(sm.isFloatingRunning());
        }
        if (switchVpn != null && switchVpn.isChecked() != sm.isVpnRunning()) {
            switchVpn.setChecked(sm.isVpnRunning());
        }
        if (switchJitter != null && switchJitter.isChecked() != sm.isJitterRunning()) {
            switchJitter.setChecked(sm.isJitterRunning());
        }
        if (switchUdp != null && switchUdp.isChecked() != sm.isUdpRunning()) {
            switchUdp.setChecked(sm.isUdpRunning());
        }
        if (switchDl != null && switchDl.isChecked() != sm.isDownloadRunning()) {
            switchDl.setChecked(sm.isDownloadRunning());
        }
        if (switchUl != null && switchUl.isChecked() != sm.isUploadRunning()) {
            switchUl.setChecked(sm.isUploadRunning());
        }

        // Estado VPN
        if (sm.isVpnRunning()) {
            if (sm.isTunnelSecured()) {
                txtVpnBadge.setText("TÚNEL PROTEGIDO");
                txtVpnBadge.setTextColor(Color.parseColor("#00E676"));
            } else {
                txtVpnBadge.setText("INSPEÇÃO LOCAL");
                txtVpnBadge.setTextColor(Color.parseColor("#00E5FF"));
            }
        } else {
            txtVpnBadge.setText("DESCONECTADO");
            txtVpnBadge.setTextColor(Color.parseColor("#FF5252"));
        }
        txtVpnDetails.setText(sm.getVpnStatusMessage());

        // Métricas de Jitter / Latência
        txtPing.setText(NetworkUtils.formatLatency(sm.getPingMs()));
        txtJitter.setText(NetworkUtils.formatJitter(sm.getJitterMs()));
        txtLoss.setText(String.format("%.1f%%", sm.getPacketLossPercent()));
        txtMinMaxAvg.setText("Min: " + NetworkUtils.formatLatency(sm.getMinPingMs()) +
                " | Max: " + NetworkUtils.formatLatency(sm.getMaxPingMs()) +
                " | Média: " + NetworkUtils.formatLatency(sm.getAvgPingMs()));

        if (sm.isNetworkUnstable()) {
            txtJitterAlert.setText("ALERTA DE OSCILAÇÃO");
            txtJitterAlert.setTextColor(Color.parseColor("#FFB300"));
        } else {
            txtJitterAlert.setText("ESTÁVEL");
            txtJitterAlert.setTextColor(Color.parseColor("#00E676"));
        }

        // Gráfico de Latência
        if (graphLatency != null) {
            graphLatency.setData(sm.getLatencyHistory(), null);
        }

        // Métricas de Tráfego
        boolean preferMb = preferences.isUnitMb();
        txtDlSpeed.setText(NetworkUtils.formatSpeed(sm.getDownloadSpeedBps(), preferMb));
        txtUlSpeed.setText(NetworkUtils.formatSpeed(sm.getUploadSpeedBps(), preferMb));
        txtDlTotal.setText("Total: " + NetworkUtils.formatBytes(sm.getTotalRxBytes()));
        txtUlTotal.setText("Total: " + NetworkUtils.formatBytes(sm.getTotalTxBytes()));

        // Gráfico de Banda
        if (graphBandwidth != null) {
            graphBandwidth.setData(sm.getDownloadHistory(), sm.getUploadHistory());
        }

        // Métricas UDP
        txtUdpPackets.setText(String.valueOf(sm.getUdpPacketCount()));
        txtUdpBytes.setText(NetworkUtils.formatBytes(sm.getUdpByteCount()));
        txtUdpFlows.setText(sm.getUdpActiveFlows() + " fluxos");
    }
}
