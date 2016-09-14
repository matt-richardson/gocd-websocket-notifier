package com.matt_richardson.gocd.websocket_notifier;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_10;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class GoCdWebSocketClient extends WebSocketClient
{
    final CountDownLatch lock = new CountDownLatch(1);
    final String[] receivedMessages = {null};
    final boolean[] connectionOpened = {false};

    public GoCdWebSocketClient(URI uri) {
        super(uri, new Draft_10());
        System.out.println("Connecting to websocket");
        connect();
    }

    @Override
    public void onMessage( String message ) {
        System.out.println("Received websocket message: " + message);
        receivedMessages[0] = message;
        lock.countDown();
    }

    @Override
    public void onOpen( ServerHandshake handshake ) {
        System.out.println("Opened connection to " + uri);
        connectionOpened[0] = true;
    }

    @Override
    public void onClose( int code, String reason, boolean remote ) {
        System.out.println("Closed websocket connection to " + uri);
        lock.countDown();
    }

    @Override
    public void onError( Exception ex ) {
        ex.printStackTrace();
        lock.countDown();
    }

    public void waitForAMessage() throws InterruptedException {
        System.out.println("Waiting for websocket message");
        lock.await(5, TimeUnit.MINUTES);
    }

    public boolean wasConnectionOpened() {
        return connectionOpened.length > 0 && connectionOpened[0];
    }

    public boolean receivedAtLeastOneMessage() {
        return receivedMessages.length == 0 || receivedMessages[0] == null;
    }
}
