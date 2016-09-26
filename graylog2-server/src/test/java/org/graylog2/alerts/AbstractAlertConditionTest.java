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
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AbstractAlertConditionTest extends AlertConditionTest {
    protected AlertCondition alertCondition;
    final protected int grace = 10;
    final protected int time = 10;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        alertCondition = getDummyAlertCondition(getParametersMap(grace, time, 0));
    }

    @Test
    @Ignore
    public void testInGracePeriod() throws Exception {
        alertLastTriggered(-1);
        assertFalse("Should not be in grace period because alert was never fired", alertService.inGracePeriod(alertCondition));
        alertLastTriggered(0);
        assertTrue("Should be in grace period because alert was just fired", alertService.inGracePeriod(alertCondition));
        alertLastTriggered(grace * 60 - 1);
        assertTrue("Should be in grace period because alert was fired during grace period", alertService.inGracePeriod(alertCondition));
        alertLastTriggered(grace * 60 + 1);
        assertFalse("Should not be in grace period because alert was fired after grace period has passed", alertService.inGracePeriod(alertCondition));
        alertLastTriggered(Integer.MAX_VALUE);
        assertFalse("Should not be in grace period because alert was fired after grace period has passed", alertService.inGracePeriod(alertCondition));

        final AlertCondition alertConditionZeroGrace = getDummyAlertCondition(getParametersMap(0, time, 0));
        alertLastTriggered(0);
        assertFalse("Should not be in grace period because grace is zero", alertService.inGracePeriod(alertConditionZeroGrace));
        alertLastTriggered(-1);
        assertFalse("Should not be in grace period because grace is zero", alertService.inGracePeriod(alertConditionZeroGrace));
        alertLastTriggered(Integer.MAX_VALUE);
        assertFalse("Should not be in grace period because grace is zero", alertService.inGracePeriod(alertConditionZeroGrace));
    }

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

    protected AlertCondition getDummyAlertCondition(Map<String, Object> parameters) {
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
