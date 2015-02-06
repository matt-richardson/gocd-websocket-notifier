package com.matt_richardson.gocd.websocket_notifier;

import com.google.gson.annotations.SerializedName;
import java.net.URI;
import java.net.URISyntaxException;

public class GoNotificationMessage
{

  @SerializedName("pipeline-name")
  private String pipelineName;

  @SerializedName("pipeline-counter")
  private String pipelineCounter;

  @SerializedName("stage-name")
  private String stageName;

  @SerializedName("stage-counter")
  private String stageCounter;

  @SerializedName("stage-state")
  private String stageState;

  @SerializedName("stage-result")
  private String stageResult;

  @SerializedName("create-time")
  private String createTime;

  @SerializedName("last-transition-time")
  private String lastTransitionTime;

  public String goServerUrl(String host)
    throws URISyntaxException
  {
    return new URI(String.format("%s/go/pipelines/%s/%s/%s/%s", new Object[] { host, this.pipelineName, this.pipelineCounter, this.stageName, this.stageCounter })).normalize().toASCIIString();
  }

  public String fullyQualifiedJobName() {
    return this.pipelineName + "/" + this.pipelineCounter + "/" + this.stageName + "/" + this.stageCounter;
  }

  public String getPipelineName() {
    return this.pipelineName;
  }

  public String getPipelineCounter() {
    return this.pipelineCounter;
  }

  public String getStageName() {
    return this.stageName;
  }

  public String getStageCounter() {
    return this.stageCounter;
  }

  public String getStageState() {
    return this.stageState;
  }

  public String getStageResult() {
    return this.stageResult;
  }

  public String getCreateTime() {
    return this.createTime;
  }

  public String getLastTransitionTime() {
    return this.lastTransitionTime;
  }
}