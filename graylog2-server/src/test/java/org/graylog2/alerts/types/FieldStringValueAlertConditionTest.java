package org.graylog2.alerts.types;

import org.graylog2.Configuration;
import org.graylog2.alerts.AlertConditionTest;
import org.graylog2.plugin.Tools;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class FieldStringValueAlertConditionTest extends AlertConditionTest {

    @Test
    public void testConstructor() throws Exception {
        final Map<String, Object> parameters = getParametersMap(0, "field", "value");

        final FieldStringValueAlertCondition condition = getCondition(parameters);

        assertNotNull(condition);
        assertNotNull(condition.getDescription());
    }

    protected FieldStringValueAlertCondition getCondition(Map<String, Object> parameters) {
        return new FieldStringValueAlertCondition(
                searches,
                mock(Configuration.class),
                stream,
                CONDITION_ID,
                Tools.iso8601(),
                STREAM_CREATOR,
                parameters);
    }

    protected Map<String, Object> getParametersMap(Integer grace, String field, String value) {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put("grace", grace);
        parameters.put("field", field);
        parameters.put("value", value);

        return parameters;
    }

}