package com.matt-richardson.gocd.websocket-notifier;

import com.thoughtworks.go.plugin.api.logging.Logger;
import com.matt-richardson.gocd.websocket-notifier.ruleset.PipelineStatus;

public abstract class PipelineListener
{
  private Logger LOG = Logger.getLoggerFor(PipelineListener.class);

  public void notify(GoNotificationMessage message)
    throws Exception
  {
    handlePipelineStatus(PipelineStatus.valueOf(message.getStageResult().toUpperCase()), message);
  }

  protected void handlePipelineStatus(PipelineStatus status, GoNotificationMessage message) throws Exception {
    switch (1.$SwitchMap$in$ashwanthkumar$gocd$slack$ruleset$PipelineStatus[status.ordinal()]) {
    case 1:
      onSuccess(message);
      break;
    case 2:
      onFailed(message);
      break;
    case 3:
      onFixed(message);
      break;
    case 4:
      onBroken(message);
      break;
    case 5:
      break;
    default:
      throw new RuntimeException("I just got pipeline status=" + status + ". I don't know how to handle it.");
    }
  }

  public abstract void onSuccess(GoNotificationMessage paramGoNotificationMessage)
    throws Exception;

  public abstract void onFailed(GoNotificationMessage paramGoNotificationMessage)
    throws Exception;

  public abstract void onBroken(GoNotificationMessage paramGoNotificationMessage)
    throws Exception;

  public abstract void onFixed(GoNotificationMessage paramGoNotificationMessage)
    throws Exception;
}