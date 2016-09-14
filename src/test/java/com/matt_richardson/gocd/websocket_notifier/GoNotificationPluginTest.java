package com.matt_richardson.gocd.websocket_notifier;

import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class GoNotificationPluginTest {

    @Test
    public void testCanHandleNotificationsInterestedInMessage() {
        GoNotificationPlugin plugin = new GoNotificationPlugin();
        GoPluginApiRequest request = getGoPluginApiRequest("notifications-interested-in");
        GoPluginApiResponse response = plugin.handle(request);

        Assert.assertEquals(200, response.responseCode());
        Assert.assertEquals("{\"notifications\":[\"stage-status\"]}", response.responseBody());
    }

    private GoPluginApiRequest getGoPluginApiRequest(final String requestName) {
        return new GoPluginApiRequest() {
            @Override
            public String extension() { return null; }

            @Override
            public String extensionVersion()  { return null; }

            @Override
            public String requestName() {
                return requestName;
            }

            @Override
            public Map<String, String> requestParameters()  { return null; }

            @Override
            public Map<String, String> requestHeaders()  { return null; }

            @Override
            public String requestBody() { return null; }
        };
    }

    @Test
    public void testCanHandleStageStatusMessage() {
        FakeWebSocketPipelineListener fakeWebSocketPipelineListener = new FakeWebSocketPipelineListener();
        GoNotificationPlugin plugin = new GoNotificationPlugin(fakeWebSocketPipelineListener);
        GoPluginApiRequest request = getGoPluginApiRequest("stage-status");
        GoPluginApiResponse response = plugin.handle(request);

        Assert.assertEquals(200, response.responseCode());
        Assert.assertEquals("{\"messages\":[],\"status\":\"success\"}", response.responseBody());
        Assert.assertEquals(request, fakeWebSocketPipelineListener.getReceivedMessage());
    }

    @Test
    public void testCanHandleUnknownMessage() {
        GoNotificationPlugin plugin = new GoNotificationPlugin();
        GoPluginApiRequest request = getGoPluginApiRequest("go.plugin-settings.get-configuration");
        GoPluginApiResponse response = plugin.handle(request);

        Assert.assertEquals(400, response.responseCode());
        Assert.assertEquals("Invalid request name: go.plugin-settings.get-configuration", response.responseBody());
    }
}
