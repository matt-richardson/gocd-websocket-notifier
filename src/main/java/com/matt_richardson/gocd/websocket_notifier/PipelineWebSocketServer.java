package com.matt_richardson.gocd.websocket_notifier ;

import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class PipelineWebSocketServer extends WebSocketServer
{
  public PipelineWebSocketServer(int port)
    throws UnknownHostException
  {
    super(new InetSocketAddress(port));
  }

  public PipelineWebSocketServer(InetSocketAddress address) {
    super(address);
  }

  public void onOpen(WebSocket conn, ClientHandshake handshake)
  {
    sendToAll("new connection: " + handshake.getResourceDescriptor());
    System.out.println(conn.getRemoteSocketAddress().getAddress().getHostAddress() + " entered the room!");
  }

  public void onClose(WebSocket conn, int code, String reason, boolean remote)
  {
    sendToAll(conn + " has left the room!");
    System.out.println(conn + " has left the room!");
  }

  public void onMessage(WebSocket conn, String message)
  {
    sendToAll(message);
    System.out.println(conn + ": " + message);
  }

  public void onError(WebSocket conn, Exception ex)
  {
    ex.printStackTrace();
    if (conn != null);
  }

  public void sendToAll(String text)
  {
    Collection<WebSocket> con = connections();
    synchronized (con) {
      for (WebSocket c : con)
        c.send(text);
    }
  }
}