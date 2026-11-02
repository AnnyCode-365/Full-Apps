package com.example.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.example.MainActivity;
import com.example.R;

/**
 * Gerenciador de canais e notificações de Foreground Service para a VPN e Painel Flutuante.
 * Compatível com Java 7.
 */
public class NotificationHelper {

    public static final String CHANNEL_VPN_ID = "ghost_vpn_channel";
    public static final String CHANNEL_FLOATING_ID = "ghost_floating_channel";

    public static final int NOTIF_ID_VPN = 1001;
    public static final int NOTIF_ID_FLOATING = 1002;

    public static void initChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                // Canal VPN
                NotificationChannel vpnChannel = new NotificationChannel(
                        CHANNEL_VPN_ID,
                        "Ghost Network VPN",
                        NotificationManager.IMPORTANCE_LOW
                );
                vpnChannel.setDescription("Status da conexão VPN Local do Ghost Network");
                vpnChannel.setShowBadge(false);
                nm.createNotificationChannel(vpnChannel);

                // Canal Painel Flutuante
                NotificationChannel floatingChannel = new NotificationChannel(
                        CHANNEL_FLOATING_ID,
                        "Ghost Network Floating Window",
                        NotificationManager.IMPORTANCE_LOW
                );
                floatingChannel.setDescription("Serviço de sobreposição e controles rápidos");
                floatingChannel.setShowBadge(false);
                nm.createNotificationChannel(floatingChannel);
            }
        }
    }

    public static Notification buildVpnNotification(Context context, String subtitle) {
        initChannels(context);

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        return new NotificationCompat.Builder(context, CHANNEL_VPN_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("GHOST NETWORK VPN")
                .setContentText(subtitle != null ? subtitle : "VPN Local Ativa e Monitorando")
                .setContentIntent(pi)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    public static Notification buildFloatingNotification(Context context, String subtitle) {
        initChannels(context);

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(
                context,
                1,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        return new NotificationCompat.Builder(context, CHANNEL_FLOATING_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("GHOST NETWORK FLOATING")
                .setContentText(subtitle != null ? subtitle : "Janela flutuante ativa sobre outros apps")
                .setContentIntent(pi)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
}
