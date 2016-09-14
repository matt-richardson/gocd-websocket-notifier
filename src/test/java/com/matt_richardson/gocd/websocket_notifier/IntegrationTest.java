package com.matt_richardson.gocd.websocket_notifier;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_10;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class IntegrationTest extends IntegrationBase {

    @Test
    public void testWebSocketReceivesMessageOnNewPipeline() throws Exception
    {
        final CountDownLatch lock = new CountDownLatch(1);
        final String[] receivedMessages = {null};
        final boolean[] connectionOpened = {false};

        WebSocketClient mWs = new WebSocketClient( new URI( "ws://localhost:" + websocketsPort ), new Draft_10() )
        {
            @Override
            public void onMessage( String message ) {
                System.out.println("Received websocket message: " + message);
                receivedMessages[0] = message;
                lock.countDown();
            }

            @Override
            public void onOpen( ServerHandshake handshake ) {
                System.out.println("Opened connection to ws://localhost:" + websocketsPort);
                connectionOpened[0] = true;
            }

            @Override
            public void onClose( int code, String reason, boolean remote ) {
                System.out.println("Closed connection");
                lock.countDown();
            }

            @Override
            public void onError( Exception ex ) {
                ex.printStackTrace();
                lock.countDown();
            }
        };

        System.out.println("Connecting to websocket");
        mWs.connect();
        System.out.println("Creating pipeline");
        String pipelineName = goCdApi.createPipeline();
        System.out.println("Un-pausing pipeline");
        goCdApi.unPausePipeline(pipelineName);
        System.out.println("Waiting for websocket message after pipeline is triggered");
        lock.await(5, TimeUnit.MINUTES);

        if (connectionOpened.length == 0 || !connectionOpened[0])
            throw new Exception("Failed to connect to websocket endpoint");

        mWs.close();
        if (receivedMessages.length == 0 || receivedMessages[0] == null)
            throw new Exception("Didn't get a message over the websocket.");
        String expectedPattern = "\\{\"pipeline\":\\{\"name\":\"" + pipelineName + "\",\"counter\":\"1\",\"group\":\"first\",\"build-cause\":\\[\\{\"material\":\\{\"type\":\"git\",\"git-configuration\":\\{\"shallow-clone\":false,\"branch\":\"master\",\"url\":\"https://github.com/matt-richardson/gocd-websocket-notifier.git\"}},\"changed\":true,\"modifications\":\\[\\{\"revision\":\"[0-9a-f]*\",\"modified-time\":\".*\",\"data\":\\{}}]}],\"stage\":\\{\"name\":\"defaultStage\",\"counter\":\"1\",\"approval-type\":\"success\",\"approved-by\":\".*\",\"state\":\"Building\",\"result\":\"Unknown\",\"create-time\":\".*\",\"last-transition-time\":\"\",\"jobs\":\\[\\{\"name\":\"defaultJob\",\"schedule-time\":\".*\",\"complete-time\":\"\",\"state\":\"Scheduled\",\"result\":\"Unknown\"}]}},\"x-pipeline-instance-details\":\\{\"build_cause\":\\{\"approver\":\".*\",\"material_revisions\":\\[\\{\"modifications\":\\[\\{\"email_address\":null,\"id\":1,\"modified_time\":\\d*,\"user_name\":\".*\",\"comment\":\".*\",\"revision\":\"[0-9a-f]*\"}],\"material\":\\{\"description\":\"URL: https://github.com/matt-richardson/gocd-websocket-notifier.git, Branch: master\",\"fingerprint\":\".*\",\"type\":\"Git\",\"id\":\\d*},\"changed\":true}],\"trigger_forced\":.*,\"trigger_message\":\".*\"},\"name\":\"" + pipelineName + "\",\"natural_order\":1.0,\"can_run\":false,\"comment\":null,\"stages\":\\[\\{\"name\":\"defaultStage\",\"approved_by\":\".*\",\"jobs\":\\[\\{\"name\":\"defaultJob\",\"result\":\"Unknown\",\"state\":\"Scheduled\",\"id\":1,\"scheduled_date\":\\d*}],\"can_run\":false,\"result\":\"Unknown\",\"approval_type\":\"success\",\"counter\":\"1\",\"id\":1,\"operate_permission\":true,\"rerun_of_counter\":null,\"scheduled\":true}],\"counter\":1,\"id\":1,\"preparing_to_schedule\":false,\"label\":\"1\"}}";
        Assert.assertTrue(receivedMessages[0].matches(expectedPattern));
    }
}
