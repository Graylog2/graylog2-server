package org.graylog.plugins.pipelineprocessor.processors;

import org.graylog2.plugin.LocalMetricRegistry;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PipelineInterpreterStateTest {

    @Test
    public void testMetricName() {
        final PipelineInterpreterState state = new PipelineInterpreterState(null, null, null,
                new LocalMetricRegistry(), 1, false);
        assertEquals("org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter.stage-cache",
                state.getStageCacheMetricName());
    }
}
