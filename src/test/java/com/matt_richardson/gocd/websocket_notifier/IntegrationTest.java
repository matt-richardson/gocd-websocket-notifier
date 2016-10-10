package com.matt_richardson.gocd.websocket_notifier;

import com.google.gson.JsonObject;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;

public class IntegrationTest extends IntegrationBase {

    @Test
    public void testWebSocketReceivesMessageOnNewPipeline() throws Exception
    {
        GoCdWebSocketClient client = new GoCdWebSocketClient( new URI( "ws://localhost:" + pluginConfig.getPort() ));

        System.out.println("Creating pipeline");
        String pipelineName = goCdApi.createPipeline();
        System.out.println("Un-pausing pipeline");
        goCdApi.unPausePipeline(pipelineName);
        client.waitForAMessage();

        if (!client.wasConnectionOpened())
            throw new Exception("Failed to connect to websocket endpoint");

        client.close();
        if (client.receivedAtLeastOneMessage())
            throw new Exception("Didn't get a message over the websocket.");

        String expectedPattern = "\\{\"pipeline\":\\{\"name\":\"" + pipelineName + "\",\"counter\":\"1\",\"group\":\"first\",\"build-cause\":\\[\\{\"material\":\\{\"type\":\"git\",\"git-configuration\":\\{\"shallow-clone\":false,\"branch\":\"master\",\"url\":\"https://github.com/matt-richardson/gocd-websocket-notifier.git\"}},\"changed\":true,\"modifications\":\\[\\{\"revision\":\"[0-9a-f]*\",\"modified-time\":\".*\",\"data\":\\{}}]}],\"stage\":\\{\"name\":\"defaultStage\",\"counter\":\"1\",\"approval-type\":\"success\",\"approved-by\":\".*\",\"state\":\"Building\",\"result\":\"Unknown\",\"create-time\":\".*\",\"last-transition-time\":\"\",\"jobs\":\\[\\{\"name\":\"defaultJob\",\"schedule-time\":\".*\",\"complete-time\":\"\",\"state\":\"Scheduled\",\"result\":\"Unknown\"}]}},\"x-pipeline-instance-details\":\\{\"build_cause\":\\{\"approver\":\".*\",\"material_revisions\":\\[\\{\"modifications\":\\[\\{\"email_address\":null,\"id\":1,\"modified_time\":\\d*,\"user_name\":\".*\",\"comment\":\".*\",\"revision\":\"[0-9a-f]*\"}],\"material\":\\{\"description\":\"URL: https://github.com/matt-richardson/gocd-websocket-notifier.git, Branch: master\",\"fingerprint\":\".*\",\"type\":\"Git\",\"id\":\\d*},\"changed\":true}],\"trigger_forced\":.*,\"trigger_message\":\".*\"},\"name\":\"" + pipelineName + "\",\"natural_order\":1.0,\"can_run\":false,\"comment\":null,\"stages\":\\[\\{\"name\":\"defaultStage\",\"approved_by\":\".*\",\"jobs\":\\[\\{\"name\":\"defaultJob\",\"result\":\"Unknown\",\"state\":\"Scheduled\",\"id\":1,\"scheduled_date\":\\d*}],\"can_run\":false,\"result\":\"Unknown\",\"approval_type\":\"success\",\"counter\":\"1\",\"id\":1,\"operate_permission\":true,\"rerun_of_counter\":null,\"scheduled\":true}],\"counter\":1,\"id\":1,\"preparing_to_schedule\":false,\"label\":\"1\"}}";
        Assert.assertTrue(client.receivedMessages[0].matches(expectedPattern));
    }

    @Test
    public void testDownloadPipelineInstanceDetails() throws Exception {
        GoCdWebSocketClient client = new GoCdWebSocketClient( new URI( "ws://localhost:" + pluginConfig.getPort() ));

        String pipelineName = goCdApi.createPipeline();
        goCdApi.unPausePipeline(pipelineName);
        goCdApi.unPausePipeline(pipelineName);
        client.waitForAMessage();

        if (!client.wasConnectionOpened())
            throw new Exception("Failed to connect to websocket endpoint");

        client.close();
        if (client.receivedAtLeastOneMessage())
            throw new Exception("Didn't get a message over the websocket.");

        PipelineDetailsPopulator populator = new PipelineDetailsPopulator(pluginConfig);
        JsonObject element = populator.downloadPipelineInstanceDetails(pipelineName).getAsJsonObject();
        Assert.assertEquals(element.get("name").getAsString(), pipelineName);
    }
}
