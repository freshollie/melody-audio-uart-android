package com.freshollie.uart.melodyaudio;

import android.util.Log;

import java.util.ArrayList;
import java.util.PriorityQueue;

/**
 * Created by freshollie on 08.12.17.
 */

public class MelodyAudioUartInterface {
    private static final String TAG = MelodyAudioUartInterface.class.getSimpleName();

    private static final char NEW_LINE_CHARACTER = '\r';

    public static class BluetoothProfiles {
        public static final int A2DP = 0;
        public static final int AVRCP = 1;
        public static final int HFPAG = 2;
        public static final int HFP = 3;
        public static final int BLE = 4;
        public static final int SPP = 5;
        public static final int PBAP = 6;
        public static final int HID = 7;
        public static final int MAP = 8;
        public static final int IAP = 9;
        public static final int TWS = 10;

        public static final String HFPAG_STRING = "HFPAG";
    }

    public static class ConfigKeys {
        public static final String NAME = "NAME";
        public static final String NAME_SHORT = "NAME_SHORT";
    }

    public static class Commands {
        public static final String RESET = "RESET";

        public static final String CONFIG = "CONFIG";
        public static final String SET = "SET";
        public static final String WRITE = "WRITE";

        public static final String LIST = "LIST";
        public static final String STATUS = "STATUS";

        public static final String NAME = "NAME";

        public static final String CALL = "CALL";
        public static final String MEDIA = "MEDIA";
    }

    public static class ResponseTypes {
        public static final String AVRCP = "AVRCP";
        public static final String CALL = "CALL";
        public static final String PB = "PB_PULL";
        public static final String OPEN = "OPEN";
        public static final String A2DP_STREAM = "A2DP_STREAM";
    }

    public static class ResponseKeys {
        public static final String READY = "READY";
        public static final String PENDING = "PENDING";

        public static final String A2DP_STREAM_START = "A2DP_STREAM_START";
        public static final String A2DP_STREAM_SUSPEND = "A2DP_STREAM_SUSPEND";

        public static final String ABS_VOL = "ABS_VOL";

        public static final String AT = "AT";

        public static final String AVRCP_MEDIA = "AVRCP_MEDIA";
        public static final String AVRCP_PLAY = "AVRCP_PLAY";
        public static final String AVRCP_STOP = "AVRCP_STOP";
        public static final String AVRCP_PAUSE = "AVRCP_PAUSE";
        public static final String AVRCP_FORWARD = "AVRCP_FORWARD";
        public static final String AVRCP_BACKWARD = "AVRCP_BACKWARD";

        public static final String CALL_ACTIVE  = "CALL_ACTIVE";
        public static final String CALL_DIAL= "CALL_DIAL";
        public static final String CALL_END = "CALL_END";
        public static final String CALL_INCOMING = "CALL_INCOMING";
        public static final String CALL_MEMORY = "CALL_MEMORY";
        public static final String CALL_OUTGOING = "CALL_OUTGOING";
        public static final String CALL_REDIAL = "CALL_REDIAL";

        public static final String CALLER_NUMBER = "CALLER_NUMBER";

        public static final String CLOSE_OK = "CLOSE_OK";
        public static final String CLOSE_ERROR = "CLOSE_ERROR";

        public static final String ERROR = "ERROR";
        public static final String OK = "OK";

        public static final String LINK_LOSS = "LINK_LOSS";

        public static final String MAP = "MAP";
        public static final String MAP_NEW_SMS = "MAP_NEW_SMS";
        public static final String MAP_MSG_BEGIN = "MAP_MSG_BEGIN";
        public static final String MAP_MSG_END = "MAP_MSG_END";

        public static final String NAME = "NAME";

        public static final String OPEN_ERROR = "OPEN_ERROR";
        public static final String OPEN_OK = "OPEN_OK";

        public static final String PAIR = "PAIR";
        public static final String PAIR_ERROR = "PAIR_ERROR";
        public static final String PAIR_OK = "PAIR_OK";
        public static final String PAIR_PASSKEY = "PAIR_PASSKEY";
        public static final String PAIR_PENDING = "PAIR_PENDING";

        public static final String PB_PULL_START = "PB_PULL_START";
        public static final String PB_PULL_END = "PB_PULL_END";
        // Pull done
        public static final String PB_PULL_OK = "PB_PULL_OK";
    }

    // Stores the current response as we receive it
    private StringBuilder responseLineBuilder;

    private final PriorityQueue<String> pendingCommands;
    private MelodyAudioUartConnection melodyAudioUartConnection;
    private final ArrayList<MelodyAudioUartInterfaceCallback> interfaceCallbacks;

    private int receivingPBDataLinkId;

    MelodyAudioUartInterface(MelodyAudioUartConnection connection) {
        melodyAudioUartConnection = connection;

        responseLineBuilder = new StringBuilder();
        receivingPBDataLinkId = -1;

        interfaceCallbacks = new ArrayList<>();
        pendingCommands = new PriorityQueue<>();
    }

    private void onOkReceived() {
        String command = pendingCommands.poll();

        synchronized (interfaceCallbacks) {
            for (MelodyAudioUartInterfaceCallback callback: interfaceCallbacks) {
                callback.onOKReceived(command);
            }
        }
    }

    private void onErrorReceived(String data) {
        String hexCode =
                data.replace(ResponseKeys.ERROR, "");
        int code = Integer.parseInt(hexCode, 16);
        String command = pendingCommands.poll();

        synchronized (interfaceCallbacks) {
            for (MelodyAudioUartInterfaceCallback callback: interfaceCallbacks) {
                callback.onErrorReceived(command, code);
            }
        }
    }

    private void onPendingReceived(String data) {
        String command = pendingCommands.poll();

        synchronized (interfaceCallbacks) {
            for (MelodyAudioUartInterfaceCallback callback: interfaceCallbacks) {
                callback.onPendingReceived(command);
            }
        }
    }

    private void onPreferenceReceived(String data) {
        String[] pair = data.split("=");
        String key = pair[0];
        String value = pair[1];

        synchronized (interfaceCallbacks) {
            for (MelodyAudioUartInterfaceCallback callback: interfaceCallbacks) {
                callback.onPreferenceReceived(key, value);
            }
        }
    }

    private void onABSVolReceived(String data) {
        String[] values = data.split(" ");
        int linkId = Integer.valueOf(values[1]);
        int volume = Integer.valueOf(values[2]);

        synchronized (interfaceCallbacks) {
            for (MelodyAudioUartInterfaceCallback callback: interfaceCallbacks) {
                callback.onABSVolReceived(linkId, volume);
            }
        }
    }

    private void onAVRCPReceived(String data) {
        String[] values = data.split(" ");
        String avrcpType = values[0];
        int linkId = Integer.valueOf(values[1]);

        synchronized (interfaceCallbacks) {
            for (MelodyAudioUartInterfaceCallback callback: interfaceCallbacks) {
                callback.onAVRCPReceived(linkId, avrcpType);
            }
        }
    }

    private void onCallStatusReceived(String data) {
        String[] values = data.split(" ");

        String callStatus = values[0];

        int linkType = values[1].equals(BluetoothProfiles.HFPAG_STRING) ?
                BluetoothProfiles.HFPAG :
                BluetoothProfiles.HFP;

        int linkId = Integer.parseInt(values[2]);

        synchronized (interfaceCallbacks) {
            for (MelodyAudioUartInterfaceCallback callback: interfaceCallbacks) {
                callback.onCallStatusReceived(linkId, linkType, callStatus);
            }
        }
    }

    private void onCallerNumberReceived(String data) {
        String[] values = data.split(" ");
        int linkId = Integer.parseInt(values[0]);
        String number = values[1];

        synchronized (interfaceCallbacks) {
            for (MelodyAudioUartInterfaceCallback callback: interfaceCallbacks) {
                callback.onCallerNumberReceived(linkId, number);
            }
        }
    }

    private void onA2DPStreamStatusReceived(String data) {
        String[] values = data.split(" ");

        String a2dpStreamStatus = values[0];
        int linkId = Integer.parseInt(values[1]);

        synchronized (interfaceCallbacks) {
            for (MelodyAudioUartInterfaceCallback callback: interfaceCallbacks) {
                callback.onA2DPStreamStatusReceived(linkId, a2dpStreamStatus);
            }
        }
    }

    private void onReceivePBPullStatusReceived(String data) {
        String[] values = data.split(" ");
        String command = values[0];
        int linkId = Integer.parseInt(values[1]);

        if (command.equals(ResponseKeys.PB_PULL_START)) {
            receivingPBDataLinkId = linkId;
        } else {
            receivingPBDataLinkId = -1;
        }

        synchronized (interfaceCallbacks) {
            for (MelodyAudioUartInterfaceCallback callback: interfaceCallbacks) {
                callback.onReceivePBPullStatusReceived(linkId, command);
            }
        }
    }

    private void onPBDataReceived(String data) {
        int linkId = receivingPBDataLinkId;

        if (data.contains(ResponseKeys.PB_PULL_END)) {
            data = data.replace(ResponseKeys.PB_PULL_END, "");
            receivingPBDataLinkId = -1;
        }

        synchronized (interfaceCallbacks) {
            for (MelodyAudioUartInterfaceCallback callback: interfaceCallbacks) {
                callback.onPBDataReceived(linkId, data);
            }
        }
    }

    private void routeResponse(String response) {
        Log.d(TAG, "Route response " + response);
        Log.d(TAG, response);

        if (receivingPBDataLinkId != -1) {
            onPBDataReceived(response);

        } else if (response.equals(MelodyAudioUartInterface.ResponseKeys.OK)) {
            onOkReceived();

        } else if (response.contains("=")) {
            // Preference response
            onPreferenceReceived(response);

        } else if (response.startsWith(ResponseKeys.ERROR)) {
            onErrorReceived(response);

        } else if (response.startsWith(ResponseKeys.PENDING)) {
            onPendingReceived(response);

        } else if (response.startsWith(ResponseTypes.AVRCP)) {
            onAVRCPReceived(response);

        } else if (response.startsWith(ResponseKeys.ABS_VOL)) {
            onABSVolReceived(response);

        } else if (response.startsWith(ResponseTypes.CALL)) {
            onCallStatusReceived(response);

        } else if (response.startsWith(ResponseKeys.CALLER_NUMBER)) {
            onCallerNumberReceived(response);

        } else if (response.startsWith(ResponseTypes.A2DP_STREAM)) {
            onA2DPStreamStatusReceived(response);

        } else if (response.startsWith(ResponseTypes.PB)) {
            onReceivePBPullStatusReceived(response);

        }
    }

    private void sendCommand(String command) {
        sendCommand(command, "");
    }

    private void sendCommand(String command, String args) {
        pendingCommands.add(command);
        melodyAudioUartConnection.sendData((command + NEW_LINE_CHARACTER).getBytes());
    }

    public void registerMelodyAudioCallback(MelodyAudioUartInterfaceCallback callback) {
        interfaceCallbacks.add(callback);
    }

    public void unregisterMelodyAudioCallback(MelodyAudioUartInterfaceCallback callback) {
        interfaceCallbacks.remove(callback);
    }

    void onNewData(byte[] bytes) {
        // We build up a line of new data until we find a
        // new line character, where we then start the next line
        //
        // Completed lines are sent to the new line interpreter
        for (byte responseByte: bytes) {
            char receivedChar = (char) responseByte;

            // We received an end of line notice,
            if (receivedChar == NEW_LINE_CHARACTER) {
                routeResponse(responseLineBuilder.toString());
                responseLineBuilder = new StringBuilder();
            } else {
                // Only append non end of line characters
                responseLineBuilder.append(receivedChar);
            }
        }
    }

    public static class MelodyAudioUartInterfaceCallback {
        public void onAVRCPReceived(int linkId, String avrcpType) {}

        public void onA2DPStreamStatusReceived(int linkId, String a2dpStreamStatus) {}

        public void onABSVolReceived(int linkId, int volume) {}

        public void onCallStatusReceived(int linkId,  int linkType, String callStatus) {}

        public void onCallerNumberReceived(int linkId, String number)  {}

        public void onPreferenceReceived(String key, String value) {}

        public void onOKReceived(String command) {}

        public void onErrorReceived(String command, int code) {}

        public void onPendingReceived(String command) {}

        public void onPBDataReceived(int linkId, String data) {}

        public void onReceivePBPullStatusReceived(int linkId, String command) {}
    }
}
