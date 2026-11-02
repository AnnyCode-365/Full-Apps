package com.example;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import com.example.utils.NotificationHelper;

/**
 * Foreground Service para manter a janela flutuante ativa sobre outros aplicativos.
 * Não interrompe a VPN nem conexões ao ser encerrado.
 * Compatível com Java 7.
 */
public class FloatingService extends Service {

    public static final String ACTION_SHOW = "com.example.floating.SHOW";
    public static final String ACTION_HIDE = "com.example.floating.HIDE";

    private FloatingWindow floatingWindow;

    @Override
    public void onCreate() {
        super.onCreate();
        floatingWindow = new FloatingWindow(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_HIDE.equals(intent.getAction())) {
            stopFloating();
            return START_NOT_STICKY;
        }

        Notification notification = NotificationHelper.buildFloatingNotification(this, "Painel de controle flutuante ativo");
        startForeground(NotificationHelper.NOTIF_ID_FLOATING, notification);

        if (floatingWindow != null) {
            floatingWindow.show();
        }

        return START_STICKY;
    }

    private void stopFloating() {
        if (floatingWindow != null) {
            floatingWindow.hide();
        }
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        stopFloating();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
