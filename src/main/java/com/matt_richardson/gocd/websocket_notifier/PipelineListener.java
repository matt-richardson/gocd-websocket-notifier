package com.matt_richardson.gocd.websocket_notifier;

import com.thoughtworks.go.plugin.api.logging.Logger;

public abstract class PipelineListener
{
  private Logger LOG = Logger.getLoggerFor(PipelineListener.class);

  public void notify(GoNotificationMessage message)
    throws Exception
  {
    handlePipelineStatus(PipelineStatus.valueOf(message.getStageResult().toUpperCase()), message);
  }

  protected void handlePipelineStatus(PipelineStatus status, com.matt_richardson.gocd.websocket_notifier.GoNotificationMessage message) throws Exception {
    switch(status){
    case PASSED:
      onSuccess(message);
      break;
    case FAILED:
      onFailed(message);
      break;
    case FIXED:
      onFixed(message);
      break;
    case BROKEN:
      onBroken(message);
      break;
    case ALL:
      break;
    case UNKNOWN:
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