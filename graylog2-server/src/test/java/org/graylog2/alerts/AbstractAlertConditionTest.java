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
package org.graylog2.alerts;

import com.google.common.collect.ImmutableMap;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.AlertCondition;
import org.junit.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AbstractAlertConditionTest extends AlertConditionTest {
    final protected int time = 10;

    @Test
    public void testDifferingTypesForNumericalParameters() throws Exception {
        final AlertCondition alertConditionWithDouble = getDummyAlertCondition(ImmutableMap.of("grace", 3.0));
        assertEquals(alertConditionWithDouble.getGrace(), 3);
        final AlertCondition alertConditionWithInteger = getDummyAlertCondition(ImmutableMap.of("grace", 3));
        assertEquals(alertConditionWithInteger.getGrace(), 3);
        final AlertCondition alertConditionWithStringDouble = getDummyAlertCondition(ImmutableMap.of("grace", "3.0"));
        assertEquals(alertConditionWithStringDouble.getGrace(), 3);
        final AlertCondition alertConditionWithStringInteger = getDummyAlertCondition(ImmutableMap.of("grace", "3"));
        assertEquals(alertConditionWithStringInteger.getGrace(), 3);
    }

    @Test
    public void testGetNumberForDifferentFormats() throws Exception {
        final AbstractAlertCondition alertCondition = (AbstractAlertCondition)getDummyAlertCondition(ImmutableMap.of("grace", 0));
        final Optional<Number> optionalForInteger = alertCondition.getNumber(1);
        assertEquals(optionalForInteger.orElse(null).intValue(), 1);
        assertEquals(optionalForInteger.orElse(null).doubleValue(), 1.0, 0.0);

        final Optional<Number> optionalForDouble = alertCondition.getNumber(42.23);
        assertEquals(optionalForDouble.orElse(null).intValue(), 42);
        assertEquals(optionalForDouble.orElse(null).doubleValue(), 42.23, 0.0);

        final Optional<Number> optionalForStringInteger = alertCondition.getNumber("17");
        assertEquals(optionalForStringInteger.orElse(null).intValue(), 17);
        assertEquals(optionalForStringInteger.orElse(null).doubleValue(), 17.0, 0.0);

        final Optional<Number> optionalForStringDouble = alertCondition.getNumber("23.42");
        assertEquals(optionalForStringDouble.orElse(null).intValue(), 23);
        assertEquals(optionalForStringDouble.orElse(null).doubleValue(), 23.42, 0.0);

        final Optional<Number> optionalForNull = alertCondition.getNumber(null);
        assertNull(optionalForNull.orElse(null));
        assertNull(optionalForNull.orElse(null));
        assertEquals(optionalForNull.orElse(1).intValue(), 1);
        assertEquals(optionalForNull.orElse(1).doubleValue(), 1.0, 0.0);
    }

    private AlertCondition getDummyAlertCondition(Map<String, Object> parameters) {
        return new AbstractAlertCondition(stream, CONDITION_ID, null, Tools.nowUTC(), STREAM_CREATOR, parameters, "Dummy Alert Condition") {
            @Override
            public String getDescription() {
                return null;
            }

            @Override
            public AlertCondition.CheckResult runCheck() {
                return null;
            }
        };
    }
}
