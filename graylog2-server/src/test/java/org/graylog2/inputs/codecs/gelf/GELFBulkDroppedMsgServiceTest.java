package org.graylog2.inputs.codecs.gelf;

import org.graylog2.inputs.diagnosis.InputDiagnosisMetrics;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.plugin.system.SimpleNodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static com.codahale.metrics.MetricRegistry.name;

@ExtendWith(MockitoExtension.class)
class GELFBulkDroppedMsgServiceTest {

    @Mock
    private InputDiagnosisMetrics inputDiagnosisMetrics;
    private GELFBulkDroppedMsgService classUnderTest;

    @BeforeEach
    void setUp() {
        classUnderTest = new GELFBulkDroppedMsgService(inputDiagnosisMetrics);
    }

    @Test
    void handleDroppedMsgOccurrence() {
        final String json = "{"
                + "\"version\": \"1.1\","
                + "\"host\": \"example.org\","
                + "\"short_message\": \"A short message that helps you identify what is going on\""
                + "}";

        final String gelfInputId = "gelfInputId";
        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));
        rawMessage.addSourceNode(gelfInputId, new SimpleNodeId("nodeId"));

        classUnderTest.handleDroppedMsgOccurrence(rawMessage);

        Mockito.verify(inputDiagnosisMetrics, Mockito.times(1)).incCount(name("org.graylog2.inputs", gelfInputId, GELFBulkDroppedMsgService.METRIC_SUFFIX));
    }
}
