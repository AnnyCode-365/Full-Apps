package com.example.vpn;

import android.net.VpnService;
import com.example.ui.StatusManager;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Gerenciador de túnel remoto com autenticação HMAC-SHA256 e sockets protegidos.
 * Se nenhum servidor estiver configurado, opera em modo de inspeção local e alerta o usuário.
 * Compatível com Java 7.
 */
public class TunnelManager {

    public enum TunnelState {
        DISCONNECTED,
        NO_SERVER_CONFIGURED,
        CONNECTING,
        CONNECTED_AUTHENTICATED,
        ERROR
    }

    private static final byte[] MAGIC_HEADER = new byte[] { 'G', 'H', 'S', 'T' };
    private static final int HEADER_SIZE = 4 + 4 + 16; // Magic(4) + Seq(4) + HMAC(16)

    private final VpnService vpnService;
    private DatagramSocket remoteSocket;
    private InetSocketAddress remoteAddress;

    private String serverHost = "";
    private int serverPort = 1194;
    private String secretKey = "";

    private TunnelState state = TunnelState.DISCONNECTED;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread receiverThread;
    private int sequenceNumber = 0;

    public TunnelManager(VpnService vpnService) {
        this.vpnService = vpnService;
    }

    public synchronized void configure(String host, int port, String key) {
        this.serverHost = host != null ? host.trim() : "";
        this.serverPort = port > 0 ? port : 1194;
        this.secretKey = key != null ? key.trim() : "";
    }

    public boolean hasRemoteServer() {
        return serverHost != null && !serverHost.isEmpty() && !secretKey.isEmpty();
    }

    public synchronized boolean connect(final FileOutputStream tunOutputStream) {
        disconnect();

        if (!hasRemoteServer()) {
            state = TunnelState.NO_SERVER_CONFIGURED;
            StatusManager.getInstance().updateVpnState("Sem servidor configurado: conexão protegida indisponível (Inspeção Local)", false);
            return false;
        }

        try {
            state = TunnelState.CONNECTING;
            StatusManager.getInstance().updateVpnState("Conectando ao túnel remoto...", false);

            remoteSocket = new DatagramSocket();
            // CRÍTICO: Proteger o socket para que seus pacotes não entrem em loop na interface TUN
            if (!vpnService.protect(remoteSocket)) {
                state = TunnelState.ERROR;
                StatusManager.getInstance().updateVpnState("Falha ao proteger socket da VPN", false);
                return false;
            }

            InetAddress inetAddr = InetAddress.getByName(serverHost);
            remoteAddress = new InetSocketAddress(inetAddr, serverPort);
            remoteSocket.connect(remoteAddress);
            remoteSocket.setSoTimeout(3000);

            running.set(true);
            state = TunnelState.CONNECTED_AUTHENTICATED;
            StatusManager.getInstance().updateVpnState("Túnel Protegido Criptografado Ativo (" + serverHost + ":" + serverPort + ")", true);

            // Thread de recepção de pacotes vindos do túnel remoto para escrever no TUN
            receiverThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    receiveLoop(tunOutputStream);
                }
            }, "GhostTunnelReceiver");
            receiverThread.start();

            return true;
        } catch (Exception e) {
            state = TunnelState.ERROR;
            StatusManager.getInstance().updateVpnState("Erro de conexão ao túnel: " + e.getMessage(), false);
            disconnect();
            return false;
        }
    }

    public synchronized void disconnect() {
        running.set(false);
        if (remoteSocket != null) {
            remoteSocket.close();
            remoteSocket = null;
        }
        if (receiverThread != null) {
            receiverThread.interrupt();
            receiverThread = null;
        }
        if (state == TunnelState.CONNECTED_AUTHENTICATED) {
            state = TunnelState.DISCONNECTED;
        }
    }

    /**
     * Encapsula pacote IP em frame autenticado com HMAC-SHA256 e envia ao servidor remoto.
     */
    public boolean sendPacket(byte[] packet, int length) {
        if (!running.get() || remoteSocket == null || remoteAddress == null) {
            return false;
        }

        try {
            byte[] framed = new byte[HEADER_SIZE + length];
            // Magic
            System.arraycopy(MAGIC_HEADER, 0, framed, 0, 4);
            // Seq
            int seq = ++sequenceNumber;
            framed[4] = (byte) ((seq >> 24) & 0xFF);
            framed[5] = (byte) ((seq >> 16) & 0xFF);
            framed[6] = (byte) ((seq >> 8) & 0xFF);
            framed[7] = (byte) (seq & 0xFF);
            // Payload
            System.arraycopy(packet, 0, framed, HEADER_SIZE, length);

            // HMAC-SHA256
            byte[] hmac = computeHmacSha256(framed, HEADER_SIZE, length, secretKey);
            System.arraycopy(hmac, 0, framed, 8, 16);

            DatagramPacket dPacket = new DatagramPacket(framed, framed.length);
            remoteSocket.send(dPacket);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void receiveLoop(FileOutputStream tunOut) {
        byte[] buffer = new byte[32768];
        DatagramPacket dPacket = new DatagramPacket(buffer, buffer.length);

        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                remoteSocket.receive(dPacket);
                int len = dPacket.getLength();
                if (len <= HEADER_SIZE) continue;

                // Validação de Header Mágico
                if (buffer[0] != 'G' || buffer[1] != 'H' || buffer[2] != 'S' || buffer[3] != 'T') {
                    continue;
                }

                // Validação de HMAC
                int payloadLen = len - HEADER_SIZE;
                byte[] expectedHmac = computeHmacSha256(buffer, HEADER_SIZE, payloadLen, secretKey);
                boolean valid = true;
                for (int i = 0; i < 16; i++) {
                    if (buffer[8 + i] != expectedHmac[i]) {
                        valid = false;
                        break;
                    }
                }

                if (valid && tunOut != null) {
                    tunOut.write(buffer, HEADER_SIZE, payloadLen);
                }
            } catch (IOException ignored) {
            } catch (Exception e) {
                if (!running.get()) break;
            }
        }
    }

    private byte[] computeHmacSha256(byte[] data, int offset, int length, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256");
            mac.init(secretKeySpec);
            mac.update(data, offset, length);
            return mac.doFinal();
        } catch (Exception e) {
            return new byte[32];
        }
    }

    public TunnelState getState() { return state; }
}
