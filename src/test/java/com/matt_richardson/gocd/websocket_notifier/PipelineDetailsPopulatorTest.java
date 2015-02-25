package com.matt_richardson.gocd.websocket_notifier;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import junit.framework.TestCase;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class PipelineDetailsPopulatorTest extends TestCase {
    public void testMergeInPipelineInstanceDetails() throws Exception {
        String originalJson = "{\"pipeline-name\":\"test\",\"pipeline-counter\":13,\"stage-name\":\"defaultStage\",\"stage-counter\":\"1\",\"stage-state\":\"Passed\",\"stage-result\":\"Passed\",\"create-time\":\"2015-02-19T07:41:21.162Z\",\"last-transition-time\":\"2015-02-19T07:41:45.576Z\"}";
        String additionalJson = "{\"build_cause\":{\"approver\":\"\",\"material_revisions\":[{\"modifications\":[{\"email_address\":null,\"id\":10,\"modified_time\":1424331650000,\"user_name\":\"Matt Richardson \u003Cmatt@redgumtech.com.au\u003E\",\"comment\":\"Update readme with details about development status\",\"revision\":\"a50098faa112d8324d9acf9ade727f837d7930dd\"}],\"material\":{\"description\":\"URL: /home/vagrant/gocd-websocket-notifier2, Branch: master\",\"fingerprint\":\"f77449ab015e3fd36c3a2088ea6338f4d4e0922f04bdcc4c5104bbacd1a72dbe\",\"type\":\"Git\",\"id\":1},\"changed\":true}],\"trigger_forced\":false,\"trigger_message\":\"modified by Matt Richardson \u003Cmatt@redgumtech.com.au\u003E\"},\"name\":\"test\",\"natural_order\":13.0,\"can_run\":true,\"comment\":null,\"stages\":[{\"name\":\"defaultStage\",\"approved_by\":\"changes\",\"jobs\":[{\"name\":\"defaultJob\",\"result\":\"Passed\",\"state\":\"Completed\",\"id\":13,\"scheduled_date\":1424331681162}],\"can_run\":true,\"result\":\"Passed\",\"approval_type\":\"success\",\"counter\":\"1\",\"id\":13,\"operate_permission\":true,\"rerun_of_counter\":null,\"scheduled\":true}],\"counter\":13,\"id\":13,\"preparing_to_schedule\":false,\"label\":\"13\"}";

        JsonParser parser = new JsonParser();
        JsonElement pipelineInstance = parser.parse(additionalJson);
        JsonElement notification = parser.parse(originalJson);

        PipelineDetailsPopulator populator = new PipelineDetailsPopulator();
        String result = populator.mergeInPipelineInstanceDetails(notification, pipelineInstance);
        assertThat(result, is("{\"pipeline-name\":\"test\",\"pipeline-counter\":13,\"stage-name\":\"defaultStage\",\"stage-counter\":\"1\",\"stage-state\":\"Passed\",\"stage-result\":\"Passed\",\"create-time\":\"2015-02-19T07:41:21.162Z\",\"last-transition-time\":\"2015-02-19T07:41:45.576Z\",\"x-pipeline-instance-details\":{\"build_cause\":{\"approver\":\"\",\"material_revisions\":[{\"modifications\":[{\"email_address\":null,\"id\":10,\"modified_time\":1424331650000,\"user_name\":\"Matt Richardson <matt@redgumtech.com.au>\",\"comment\":\"Update readme with details about development status\",\"revision\":\"a50098faa112d8324d9acf9ade727f837d7930dd\"}],\"material\":{\"description\":\"URL: /home/vagrant/gocd-websocket-notifier2, Branch: master\",\"fingerprint\":\"f77449ab015e3fd36c3a2088ea6338f4d4e0922f04bdcc4c5104bbacd1a72dbe\",\"type\":\"Git\",\"id\":1},\"changed\":true}],\"trigger_forced\":false,\"trigger_message\":\"modified by Matt Richardson <matt@redgumtech.com.au>\"},\"name\":\"test\",\"natural_order\":13.0,\"can_run\":true,\"comment\":null,\"stages\":[{\"name\":\"defaultStage\",\"approved_by\":\"changes\",\"jobs\":[{\"name\":\"defaultJob\",\"result\":\"Passed\",\"state\":\"Completed\",\"id\":13,\"scheduled_date\":1424331681162}],\"can_run\":true,\"result\":\"Passed\",\"approval_type\":\"success\",\"counter\":\"1\",\"id\":13,\"operate_permission\":true,\"rerun_of_counter\":null,\"scheduled\":true}],\"counter\":13,\"id\":13,\"preparing_to_schedule\":false,\"label\":\"13\"}}"));
    }

    //TODO: fix, as not a great test - external dependency on a running go instance
    public void testDownloadPipelineInstanceDetails() throws Exception {
        PipelineDetailsPopulator populator = new PipelineDetailsPopulator();
        JsonElement element = populator.downloadPipelineInstanceDetails("test");
        assertThat(element, notNullValue());
    }
}
