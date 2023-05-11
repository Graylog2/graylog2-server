package org.graylog.plugins.pipelineprocessor.simulator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.plugins.pipelineprocessor.processors.ConfigurationStateUpdater;
import org.graylog2.plugin.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;

import java.util.Map;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RuleSimulatorTest {

    private RuleSimulator ruleSimulator;
    private ObjectMapper objectMapper;

    @Mock
    private ConfigurationStateUpdater configurationStateUpdater;

    @BeforeAll
    public void setUp() {
        objectMapper = new ObjectMapper();
        ruleSimulator = new RuleSimulator(configurationStateUpdater, objectMapper);
    }

    @Test
    void createMessage() {
        String notAJsonMessage = "{Not a json message}";
        Message result = ruleSimulator.createMessage(notAJsonMessage);
        Assertions.assertEquals(result.getMessage(), notAJsonMessage);
    }

    @Test
    void createMessageFromJson() {
        String jsonMessage = """
        {
            "message": "This is a test message",
            "additionalField": "this is an additional field passed"
        }
        """;
        Message result = ruleSimulator.createMessage(jsonMessage);
        Map<String, Object> fields = result.getFields();
        Assertions.assertEquals("This is a test message", fields.get("message"));
        Assertions.assertEquals("this is an additional field passed", fields.get("additionalField"));
    }
}
