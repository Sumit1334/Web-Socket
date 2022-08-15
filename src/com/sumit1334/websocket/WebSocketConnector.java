package com.sumit1334.websocket;

import android.app.Activity;
import android.util.Log;

import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.Component;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.OnDestroyListener;
import com.google.appinventor.components.runtime.util.YailDictionary;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class WebSocketConnector extends AndroidNonvisibleComponent implements Component, OnDestroyListener {
    private final Activity activity;
    private final String TAG = "WebSocket Connector";
    private final OkHttpClient client;
    private WebSocket webSocket;
    private boolean isConnected;
    private YailDictionary headers;

    public WebSocketConnector(ComponentContainer container) {
        super(container.$form());
        this.activity = container.$context();
        container.$form().registerForOnDestroy(this);
        client = new OkHttpClient();
        isConnected = false;
        Headers(YailDictionary.makeDictionary());
        Log.i(TAG, "WebSocket: Extension Initialized");
    }

    @SimpleProperty
    public void Headers(YailDictionary list) {
        this.headers = list;
    }

    @SimpleProperty
    public YailDictionary Headers() {
        return headers;
    }

    @SimpleEvent
    public void ErrorOccurred(String error) {
        EventDispatcher.dispatchEvent(this, "ErrorOccurred", error);
    }

    @SimpleEvent
    public void Connected(final String response) {
        EventDispatcher.dispatchEvent(this, "Connected", response);
    }

    @SimpleEvent
    public void Disconnected(final String reason) {
        EventDispatcher.dispatchEvent(this, "Disconnected", reason);
    }

    @SimpleEvent
    public void MessageReceived(final String message) {
        EventDispatcher.dispatchEvent(this, "MessageReceived", message);
    }

    @SimpleFunction
    public void Connect(final String url) {
        if (!url.startsWith("ws"))
            return;
        this.connect(url);
    }

    @SimpleFunction
    public void Disconnect() {
        if (webSocket != null)
            this.disconnect();
        else
            ErrorOccurred("WebSocket is not connected yet");
    }

    @SimpleFunction
    public void SendMessage(final String message) {
        if (webSocket != null)
            webSocket.send(message);
        else
            ErrorOccurred("WebSocket is not connected yet");
    }

    @SimpleFunction
    public boolean IsConnected() {
        return isConnected;
    }

    @Override
    public void onDestroy() {
        isConnected = false;
        disconnect();
    }

    private void connect(String url) {
        try {
            Request.Builder request = new Request.Builder()
                    .url(url);
            if (!headers.isEmpty()) {
                for (Object header : headers.keySet()) {
                    Log.i(TAG, "connect: I am setting header with the key : " + header);
                    request.addHeader((String) header, (String) headers.get(header));
                }
            }
            this.webSocket = client.newWebSocket(request.build(), new SocketListener());
            Log.i(TAG, "connectSocket: Trying to connect socket");
        } catch (Exception e) {
            isConnected = false;
            ErrorOccurred(e.getMessage());
            Log.e(TAG, "connection to socket failed due to " + e.getMessage());
        }

    }

    private void disconnect() {
        if (webSocket != null)
            webSocket.close(1000, "Disconnected by the user");
    }


    class SocketListener extends WebSocketListener {
        private static final int NORMAL_CLOSURE_STATUS = 1000;

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            isConnected = true;
            Log.i(TAG, "WebSocket connection has made or connected");
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Connected(response.toString());
                }
            });
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            isConnected = true;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MessageReceived(text);
                }
            });
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            isConnected = false;
            Log.i(TAG, "Closing the socket connection due to " + reason);
            webSocket.close(NORMAL_CLOSURE_STATUS, null);
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Disconnected(reason);
                }
            });
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            isConnected = false;
            Log.e(TAG, "Failed to connect due to " + t.getMessage());
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ErrorOccurred(t.getMessage());
                }
            });
        }
    }
}