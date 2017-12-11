package com.freshollie.uart.melodyaudio;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.freshollie.bluetooth.melodyaudio.BuildConfig;
import com.freshollie.bluetooth.melodyaudio.R;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

/**
 * Created by freshollie on 08.12.17.
 */

public class MelodyAudioUartConnection {
    public static final String TAG = MelodyAudioUartConnection.class.getSimpleName();

    private static final int WAIT_FOR_ATTACH_TIMEOUT = 5000;

    // These depends on your FTDI chip
    public static final int PRODUCT_ID = 24577;
    public static final int VENDOR_ID = 1027;

    public static final int STATE_CONNECTED = 3;
    public static final int STATE_RECONNECTING = 2;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_DISCONNECTED = 0;

    private static final int NOTIFICATION_ID = 1;
    private static final String NOTIFICATION_CHANNEL_ID = "USB_CONNECTION";

    private static final String ACTION_USB_PERMISSION =
            "com.freshollie.uart.melodyaudio.action.USB_PERMISSION";

    private final Context context;

    private final NotificationCompat.Builder notificationBuilder;
    private final PendingIntent usbPermissionIntent;

    private final IntentFilter intentFilter;

    private final UsbManager usbManager;
    private final NotificationManager notificationManager;

    private Handler mainThread;

    private boolean running = false;
    private boolean showNotification = false;
    private int connectionState;

    private int baudRate;

    private MelodyAudioUartInterface melodyAudioUartInterface;

    private SerialConnection serialConnection;
    private OpenConnectionThread openConnectionThread;

    private final ArrayList<ConnectionStateChangeListener> connectionStateChangeListeners = new ArrayList<>();

    public interface ConnectionStateChangeListener {
        void onChange(int newState);
    }

    /**
     * This receiver is called when a usb usbDevice is detached and when a usb
     * permission is granted or denied.
     */
    private BroadcastReceiver usbBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            log("Intent Received: " + intent.getAction());

            if (ACTION_USB_PERMISSION.equals(intent.getAction())) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            log("Permission for usbDevice granted");
                            attemptConnection();
                        }
                    } else {
                        if (device != null) {
                            log("Permission for usbDevice denied");
                            close();
                        }
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(intent.getAction())) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null && isRunning()) {
                    if (device.getVendorId() == VENDOR_ID &&
                            device.getProductId() == PRODUCT_ID) {
                        attemptReopenConnection();
                    }
                }
            }
        }
    };


    public MelodyAudioUartConnection(Context appContext, int baud) {
        Log.v(TAG, "Created");
        context = appContext;

        baudRate = baud;

        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        connectionState = STATE_DISCONNECTED;

        melodyAudioUartInterface = new MelodyAudioUartInterface(this);
        mainThread = new Handler(context.getMainLooper());

        // Notification channel for android devices larger than Oreo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            CharSequence name = context.getString(R.string.notification_channel_name);
            String description = context.getString(R.string.notification_channel_description);

            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    name,
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(description);
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            notificationManager.createNotificationChannel(channel);
        }

        notificationBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(context.getString(R.string.notification_connected_text))
                .setSmallIcon(R.drawable.ic_notification_melody_connection)
                .setOngoing(true);


        usbPermissionIntent =
                PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);

        intentFilter = new IntentFilter(ACTION_USB_PERMISSION);
        intentFilter.addAction(ACTION_USB_PERMISSION);
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
    }

    private UsbDevice getUsbSerialDevice() {
        for (UsbDevice device: usbManager.getDeviceList().values()) {
            Log.e(TAG, device.toString());
            if (device.getProductId() == PRODUCT_ID &&
                    device.getVendorId() == VENDOR_ID) {
                return device;
            }
        }

        return null;
    }

    private void attemptConnection() {
        if (openConnectionThread == null || !openConnectionThread.isAlive()) {
            openConnectionThread = new OpenConnectionThread();
            openConnectionThread.start();
        }
    }

    private void attemptReopenConnection() {
        setConnectionState(STATE_RECONNECTING);

        showReconnectingNotification();
        closeConnection();

        log("Attempting to reconnect");
        attemptConnection();
    }

    private void closeConnection() {
        log("Closing connection to usbDevice");

        if (openConnectionThread != null && openConnectionThread.isAlive()) {
            openConnectionThread.interrupt();
        }
        openConnectionThread = null;

        if (serialConnection != null && serialConnection.isOpen()) {
            serialConnection.close();
        }

        serialConnection = null;
    }

    private boolean isDeviceAttached() {
        return getUsbSerialDevice() != null;
    }

    public void setBaudRate(int baud) {
        baudRate = baud;
    }

    public void open() {
        // Start usbDevice service if the usbDevice service is currently not running
        if (!running) {
            log("Opening");
            context.registerReceiver(usbBroadcastReceiver, intentFilter);
            running = true;

            setConnectionState(STATE_CONNECTING);
            showConnectingNotification();

            attemptConnection();
        }
    }

    /**
     * Closes the connection  and closes usbDevice connection
     */
    public void close() {
        if (running) {
            running = false;
            log("Closing");

            context.unregisterReceiver(usbBroadcastReceiver);

            if (connectionState != STATE_DISCONNECTED) {
                setConnectionState(STATE_DISCONNECTED);
                closeConnection();
                showDisconnectedNotification();
            }

            cancelNotification();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public int getConnectionState() {
        return connectionState;
    }

    private void setConnectionState(int state) {
        connectionState = state;
        notifyConnectionStateChange(connectionState);
    }

    private void notifyConnectionStateChange(final int state) {
        synchronized (connectionStateChangeListeners) {
            for (final ConnectionStateChangeListener changeListener: connectionStateChangeListeners) {
                mainThread.post(new Runnable() {
                    @Override
                    public void run() {
                        changeListener.onChange(state);
                    }
                });
            }
        }
    }

    public MelodyAudioUartInterface getInterface() {
        return melodyAudioUartInterface;
    }

    public void registerConnectionChangeListener(ConnectionStateChangeListener listener) {
        connectionStateChangeListeners.add(listener);
    }

    public void unregisterConnectionChangeListener(ConnectionStateChangeListener listener) {
        connectionStateChangeListeners.remove(listener);
    }

    public void setShowNotifications(boolean show) {
        showNotification = show;
    }

    private void cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private void showConnectedNotification() {
        if (showNotification) {
            Notification notification = notificationBuilder
                    .setContentText(context.getString(R.string.notification_connected_text))
                    .setOngoing(true)
                    .build();
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    private void showDisconnectedNotification() {
        // Stub
    }

    private void showConnectingNotification() {
        if (showNotification) {
            Notification notification = notificationBuilder
                    .setContentText(context.getString(R.string.notification_text_connecting))
                    .build();
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    private void showReconnectingNotification() {
        if (showNotification) {
            Notification notification = notificationBuilder
                    .setContentText(context.getString(R.string.notification_text_connection_issues))
                    .build();
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    void sendData(byte[] data) {
        if (serialConnection != null) {
            serialConnection.write(data);
        }
    }

    private class OpenConnectionThread extends Thread {
        private final String TAG = OpenConnectionThread.class.getSimpleName();
        @Override
        public void run() {
            log("Started");

            long startTime = System.currentTimeMillis();

            while ((System.currentTimeMillis() - startTime) < WAIT_FOR_ATTACH_TIMEOUT && !interrupted()) {
                if (isDeviceAttached()) {
                    // The usbDevice is already marked as connected, so this is probably a reconnect
                    if (connectionState == STATE_RECONNECTING) {
                        // Wait 500ms as this will help with reconnection
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            return;
                        }
                    }

                    requestConnection();
                    return;
                }
            }

            if ((System.currentTimeMillis() - startTime) >= WAIT_FOR_ATTACH_TIMEOUT) {
                log("Waited too long for usbDevice to attach");
                close();
            }
        }

        private void requestConnection() {
            openConnectionThread = null;

            log("Requesting connection to usbDevice");

            UsbDevice device = getUsbSerialDevice();
            if (device != null) {
                if (usbManager.hasPermission(device)) {
                    openConnection(device);
                } else {
                    log("Requesting permission for usbDevice");
                    usbManager.requestPermission(device, usbPermissionIntent);
                }
                return;
            }

            log("No devices found");
            close();
        }

        private void openConnection(UsbDevice usbDevice) {
            if (!running) {
                // Don't open the connection if the usb
                return;
            }

            log("Opening connection to usbDevice");
            if (usbDevice != null) {
                UsbDeviceConnection connection = usbManager.openDevice(usbDevice);
                serialConnection = new SerialConnection(connection, usbDevice);
                serialConnection.open();

                setConnectionState(STATE_CONNECTED);
                showConnectedNotification();
                return;
            }

            log("Error opening connection");
            close();
        }

        private void log(String message) {
            if (BuildConfig.DEBUG) Log.d(TAG, message);
        }
    }

    private class SerialConnection implements UsbSerialInterface.UsbReadCallback {
        private final String TAG = SerialConnection.class.getSimpleName();

        private UsbSerialDevice serialDevice;
        private boolean open = false;

        SerialConnection(UsbDeviceConnection connection, UsbDevice device) {
            serialDevice = UsbSerialDevice.createUsbSerialDevice(device, connection);
        }

        void write(byte[] bytes) {
            if (open) {
                try {
                    log("Writing: " + new String(bytes, "ASCII"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                serialDevice.write(bytes);
            }
        }

        @Override
        public void onReceivedData(byte[] bytes) {
            if (open) {
                melodyAudioUartInterface.onNewData(bytes);
            }
        }

        void open() {
            if (serialDevice != null) {
                open = true;
                serialDevice.open();
                serialDevice.setBaudRate(baudRate);
                serialDevice.read(this);
            }
        }

        void close() {
            if (serialDevice != null) {
                serialDevice.close();
                open = false;
                serialDevice = null;
            }
        }

        boolean isOpen() {
            return open;
        }

        private void log(String message) {
            if (BuildConfig.DEBUG) Log.d(TAG, message);
        }
    }

    private void log(String message) {
        if (BuildConfig.DEBUG) Log.d(TAG, message);
    }
}
