package com.matt_richardson.gocd.websocket_notifier;

import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;

public class WebSocketPipelineListener {
    private PipelineWebSocketServer webSocketServer;
    private Logger LOGGER = Logger.getLoggerFor(WebSocketPipelineListener.class);

    public WebSocketPipelineListener(PipelineWebSocketServer webSocketServer) {
        this.webSocketServer = webSocketServer;
    }

    public void notify(GoPluginApiRequest message)
            throws Exception {
        LOGGER.info("notify called with request name '" + message.requestName() + "' and requestBody '" + message.requestBody() + "'");
        this.webSocketServer.sendToAll(message.requestBody());
    }
}