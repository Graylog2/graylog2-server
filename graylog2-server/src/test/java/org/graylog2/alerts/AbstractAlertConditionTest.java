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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

public class AbstractAlertConditionTest extends AlertConditionTest {
    @Test
    public void testDifferingTypesForNumericalParameters() throws Exception {
        final AlertCondition alertConditionWithDouble = getDummyAlertCondition(ImmutableMap.of("grace", 3.0));
        assertEquals(3, alertConditionWithDouble.getGrace());
        final AlertCondition alertConditionWithInteger = getDummyAlertCondition(ImmutableMap.of("grace", 3));
        assertEquals(3, alertConditionWithInteger.getGrace());
        final AlertCondition alertConditionWithStringDouble = getDummyAlertCondition(ImmutableMap.of("grace", "3.0"));
        assertEquals(3, alertConditionWithStringDouble.getGrace());
        final AlertCondition alertConditionWithStringInteger = getDummyAlertCondition(ImmutableMap.of("grace", "3"));
        assertEquals(3, alertConditionWithStringInteger.getGrace());
    }

    @Test
    public void testQueryFilterBuilder() {
        final AbstractAlertCondition condition = (AbstractAlertCondition) getDummyAlertCondition(ImmutableMap.of());

        assertThatThrownBy(() -> condition.buildQueryFilter(null, null))
                .hasMessageContaining("streamId")
                .hasMessageContaining("be null");
        assertThatThrownBy(() -> condition.buildQueryFilter("", null))
                .hasMessageContaining("streamId")
                .hasMessageContaining("be empty");

        assertThat(condition.buildQueryFilter("  abc123 ", null))
                .isEqualTo("streams:abc123");
        assertThat(condition.buildQueryFilter("abc123", ""))
                .isEqualTo("streams:abc123");
        assertThat(condition.buildQueryFilter("abc123", "*"))
                .isEqualTo("streams:abc123");
        assertThat(condition.buildQueryFilter("abc123", " *  "))
                .isEqualTo("streams:abc123");
        assertThat(condition.buildQueryFilter("abc123", " hello:world foo:\"bar baz\"   "))
                .isEqualTo("streams:abc123 hello:world foo:\"bar baz\"");
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
