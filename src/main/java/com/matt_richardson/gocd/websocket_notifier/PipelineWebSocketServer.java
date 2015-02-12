package com.matt_richardson.gocd.websocket_notifier;

import com.thoughtworks.go.plugin.api.logging.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;

public class PipelineWebSocketServer extends WebSocketServer {
    private Logger LOGGER = Logger.getLoggerFor(PipelineWebSocketServer.class);

    public PipelineWebSocketServer(int port)
            throws UnknownHostException {
        super(new InetSocketAddress(port));
    }

    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        sendToAll("new connection: " + handshake.getResourceDescriptor());
        LOGGER.info(conn.getRemoteSocketAddress().getAddress().getHostAddress() + " entered the room!");
    }

    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        sendToAll(conn + " has left the room!");
        LOGGER.info(conn + " has left the room!");
    }

    public void onMessage(WebSocket conn, String message) {
        sendToAll(message);
        LOGGER.info(conn + ": " + message);
    }

    public void onError(WebSocket conn, Exception ex) {
        LOGGER.info("websocket error", ex);
    }

    public void sendToAll(String text) {
        Collection<WebSocket> con = connections();
        synchronized (con) {
            for (WebSocket c : con) {
                LOGGER.debug("sending '" + text + "' to " + con);
                c.send(text);
            }
        }
    }
}