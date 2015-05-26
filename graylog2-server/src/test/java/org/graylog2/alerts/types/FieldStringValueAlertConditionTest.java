/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
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