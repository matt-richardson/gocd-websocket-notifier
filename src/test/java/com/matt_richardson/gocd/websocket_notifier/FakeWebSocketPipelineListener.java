package com.matt_richardson.gocd.websocket_notifier;


import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;

public class FakeWebSocketPipelineListener extends WebSocketPipelineListener {
    private GoPluginApiRequest receivedMessage;

    public FakeWebSocketPipelineListener() {
        super(null, new PluginConfig());
    }

    @Override
    public void notify(GoPluginApiRequest message) throws Exception {
        this.receivedMessage = message;
    }

    public GoPluginApiRequest getReceivedMessage() {
        return this.receivedMessage;
    }
}
