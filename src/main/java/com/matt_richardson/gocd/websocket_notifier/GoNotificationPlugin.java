package com.matt_richardson.gocd.websocket_notifier;

import com.google.gson.GsonBuilder;
import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;

import java.net.UnknownHostException;
import java.util.*;

@Extension
public class GoNotificationPlugin implements GoPlugin {
    private static Logger LOGGER = Logger.getLoggerFor(GoNotificationPlugin.class);
    public static final String EXTENSION_TYPE = "notification";
    private static final List<String> goSupportedVersions = Arrays.asList("1.0");
    public static final String REQUEST_NOTIFICATIONS_INTERESTED_IN = "notifications-interested-in";
    public static final String REQUEST_STAGE_STATUS = "stage-status";
    public static final int SUCCESS_RESPONSE_CODE = 200;
    public static final int INTERNAL_ERROR_RESPONSE_CODE = 500;
    private WebSocketPipelineListener pipelineListener;

    public void initializeGoApplicationAccessor(GoApplicationAccessor goApplicationAccessor) {
        if (pipelineListener == null) {
            PluginConfig pluginConfig = new PluginConfig();
            int port = pluginConfig.getPort();
            String host = pluginConfig.getHost();

            //org.java_websocket.WebSocketImpl.DEBUG = true;
            PipelineWebSocketServer s;
            try {
                LOGGER.info("Starting WebSocket server started on port: " + port);
                if (pluginConfig.isHostSet()) {
                	s = new PipelineWebSocketServer(host, port);
                } else {
                	s = new PipelineWebSocketServer(port);
                }
                s.start();
                LOGGER.info("WebSocket server started on port: " + s.getPort());
                pipelineListener = new WebSocketPipelineListener(s, pluginConfig);
            } catch (UnknownHostException e) {
                LOGGER.error("Failed to launch WebSocket server on port: " + port, e);
            }
        }
    }

    public GoNotificationPlugin() {

    }

    public GoNotificationPlugin(WebSocketPipelineListener pipelineListener) {
        this.pipelineListener = pipelineListener;
    }

    public GoPluginApiResponse handle(GoPluginApiRequest goPluginApiRequest) {
    	try {
	        LOGGER.debug("received go plugin api request " + goPluginApiRequest.requestName());
	        if (goPluginApiRequest.requestName().equals(REQUEST_NOTIFICATIONS_INTERESTED_IN))
	            return handleNotificationsInterestedIn();
	        if (goPluginApiRequest.requestName().equals(REQUEST_STAGE_STATUS)) {
	            return handleStageNotification(goPluginApiRequest);
	        }
	        return DefaultGoPluginApiResponse.badRequest("Invalid request name: " + goPluginApiRequest.requestName());
    	} catch (Exception e) {
    		LOGGER.error(String.format("Failed to handle request with name: %s , and body: %s ", goPluginApiRequest.requestName(), goPluginApiRequest.requestBody()), e);
    		return DefaultGoPluginApiResponse.error(e.getMessage());
    	}
    }

    public GoPluginIdentifier pluginIdentifier() {
        LOGGER.debug("received pluginIdentifier request");
        return new GoPluginIdentifier(EXTENSION_TYPE, goSupportedVersions);
    }

    private GoPluginApiResponse handleNotificationsInterestedIn() {
        Map<String, List<String>> response = new HashMap<String, List<String>>();
        response.put("notifications", Arrays.asList(REQUEST_STAGE_STATUS));
        LOGGER.debug("requesting details of stage-status notifications");
        return renderJSON(SUCCESS_RESPONSE_CODE, response);
    }

    private GoPluginApiResponse handleStageNotification(GoPluginApiRequest goPluginApiRequest) {
        LOGGER.debug("handling stage notification");

        int responseCode = SUCCESS_RESPONSE_CODE;

        Map<String, Object> response = new HashMap<String, Object>();
        List<String> messages = new ArrayList<String>();
        try {
            response.put("status", "success");
            pipelineListener.notify(goPluginApiRequest);
        } catch (Exception e) {
            LOGGER.error("failed to notify pipeline listener", e);
            responseCode = INTERNAL_ERROR_RESPONSE_CODE;
            response.put("status", "failure");
            messages.add(e.getMessage());
        }

        response.put("messages", messages);
        return renderJSON(responseCode, response);
    }

    private GoPluginApiResponse renderJSON(final int responseCode, Object response) {
        final String json = response == null ? null : new GsonBuilder().create().toJson(response);
        return new GoPluginApiResponse() {
            public int responseCode() {
                return responseCode;
            }

            public Map<String, String> responseHeaders() {
                return null;
            }

            public String responseBody() {
                return json;
            }
        };
    }
}
