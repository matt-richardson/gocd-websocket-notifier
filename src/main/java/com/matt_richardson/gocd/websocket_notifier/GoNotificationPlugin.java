package com.matt_richardson.gocd.websocket_notifier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import java.io.PrintStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Extension
public class GoNotificationPlugin
  implements GoPlugin
{
  private static Logger LOGGER = Logger.getLoggerFor(GoNotificationPlugin.class);
  public static final String EXTENSION_TYPE = "notification";
  private static final List<String> goSupportedVersions = Arrays.asList(new String[] { "1.0" });
  public static final String REQUEST_NOTIFICATIONS_INTERESTED_IN = "notifications-interested-in";
  public static final String REQUEST_STAGE_STATUS = "stage-status";
  public static final int SUCCESS_RESPONSE_CODE = 200;
  public static final int INTERNAL_ERROR_RESPONSE_CODE = 500;
  public static final String GO_NOTIFY_CONFIGURATION = "go_notify.conf";
  private PipelineListener pipelineListener;

  public void initializeGoApplicationAccessor(GoApplicationAccessor goApplicationAccessor)
  {
    org.java_websocket.WebSocketImpl.DEBUG = true;
    int port = 8887;
    PipelineWebSocketServer s = null;
    try {
      s = new PipelineWebSocketServer(port);
      s.start();
      System.out.println("WebSocket server started on port: " + s.getPort());
      this.pipelineListener = new WebSocketPipelineListener(s);
    } catch (UnknownHostException e) {
      System.out.println("Failed to launch WebSocket server started on port: " + port);
      e.printStackTrace();
    }
  }

  public GoPluginApiResponse handle(GoPluginApiRequest goPluginApiRequest)
  {
    if (goPluginApiRequest.requestName().equals("notifications-interested-in"))
      return handleNotificationsInterestedIn();
    if (goPluginApiRequest.requestName().equals("stage-status")) {
      return handleStageNotification(goPluginApiRequest);
    }
    return null;
  }

  public GoPluginIdentifier pluginIdentifier()
  {
    return new GoPluginIdentifier("notification", goSupportedVersions);
  }

  private GoPluginApiResponse handleNotificationsInterestedIn() {
    Map response = new HashMap();
    response.put("notifications", Arrays.asList(new String[] { "stage-status" }));
    return renderJSON(200, response);
  }

  private GoPluginApiResponse handleStageNotification(GoPluginApiRequest goPluginApiRequest) {
    GoNotificationMessage message = parseNotificationMessage(goPluginApiRequest);
    int responseCode = 200;

    Map response = new HashMap();
    List messages = new ArrayList();
    try {
      response.put("status", "success");
      LOGGER.info(message.fullyQualifiedJobName() + " has " + message.getStageState() + "/" + message.getStageResult());
      this.pipelineListener.notify(message);
    } catch (Exception e) {
      responseCode = 500;
      response.put("status", "failure");
      messages.add(e.getMessage());
    }

    response.put("messages", messages);
    return renderJSON(responseCode, response);
  }

  private GoNotificationMessage parseNotificationMessage(GoPluginApiRequest goPluginApiRequest) {
    return new GsonBuilder().create().fromJson(goPluginApiRequest.requestBody(), GoNotificationMessage.class);
  }

  private GoPluginApiResponse renderJSON(final int responseCode, Object response) {
    final String json = response == null ? null : new GsonBuilder().create().toJson(response);
    return new GoPluginApiResponse()
    {
      public int responseCode() {
        return responseCode;
      }

      public Map<String, String> responseHeaders()
      {
        return null;
      }

      public String responseBody()
      {
        return json;
      }
    };
  }
}