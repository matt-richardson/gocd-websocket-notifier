package com.matt-richardson.gocd.websocket-notifier;

public class WebSocketPipelineListener extends PipelineListener
{
  private PipelineWebSocketServer webSocketServer;

  public WebSocketPipelineListener(PipelineWebSocketServer webSocketServer)
  {
    this.webSocketServer = webSocketServer;
  }

  public void onSuccess(GoNotificationMessage message) throws Exception
  {
    this.webSocketServer.sendToAll(message.toString());
  }

  public void onFailed(GoNotificationMessage message) throws Exception
  {
    this.webSocketServer.sendToAll(message.toString());
  }

  public void onBroken(GoNotificationMessage message) throws Exception
  {
    this.webSocketServer.sendToAll(message.toString());
  }

  public void onFixed(GoNotificationMessage message) throws Exception
  {
    this.webSocketServer.sendToAll(message.toString());
  }
}