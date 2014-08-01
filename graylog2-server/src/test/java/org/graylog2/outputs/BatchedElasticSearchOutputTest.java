package org.graylog2.outputs;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Lists;
import org.graylog2.Configuration;
import org.graylog2.indexer.Indexer;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.mockito.ArgumentMatcher;
import org.testng.annotations.Test;

import java.util.List;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;
import static org.testng.Assert.fail;

public class BatchedElasticSearchOutputTest {

    @Test
    public void flushingBatchWritesBulk() {

        Indexer indexer = mock(Indexer.class);
        Configuration config = mock(Configuration.class);
        when(config.getOutputBatchSize()).thenReturn(10);
        MetricRegistry metricRegistry = new MetricRegistry();

        final Message msg1 = new Message("message1", "test", Tools.iso8601());
        final Message msg2 = new Message("message2", "test", Tools.iso8601());
        final Message msg3 = new Message("message3", "test", Tools.iso8601());

        ArgumentMatcher<List<Message>> isMessageList = new ArgumentMatcher<List<Message>>() {
            @Override
            public boolean matches(Object argument) {
                if (!(argument instanceof List)) {
                    return false;
                }
                return ((List) argument).containsAll(Lists.newArrayList(msg1, msg2, msg3));
            }
        };

        BatchedElasticSearchOutput output = new BatchedElasticSearchOutput(metricRegistry, indexer, config);

        try {
            output.write(msg1);
            output.write(msg2);
            output.write(msg3);
        } catch (Exception e) {
            fail("output should not throw", e);
        }

        output.flush(false);

        verify(indexer, times(1)).bulkIndex(argThat(isMessageList));

    }



}