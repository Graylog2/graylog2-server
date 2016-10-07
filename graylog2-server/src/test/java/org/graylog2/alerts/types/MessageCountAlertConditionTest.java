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

import org.graylog2.alerts.AlertConditionTest;
import org.graylog2.indexer.InvalidRangeFormatException;
import org.graylog2.indexer.results.CountResult;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.AlertCondition;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MessageCountAlertConditionTest extends AlertConditionTest {
    private final int threshold = 100;

    @Test
    public void testConstructor() throws Exception {
        final Map<String, Object> parameters = getParametersMap(0, 0, MessageCountAlertCondition.ThresholdType.MORE, 0);

        final MessageCountAlertCondition messageCountAlertCondition = getMessageCountAlertCondition(parameters, alertConditionTitle);

        assertNotNull(messageCountAlertCondition);
        assertNotNull(messageCountAlertCondition.getDescription());
    }

    @Test
    public void testRunCheckMorePositive() throws Exception {
        final MessageCountAlertCondition.ThresholdType type = MessageCountAlertCondition.ThresholdType.MORE;

        final MessageCountAlertCondition messageCountAlertCondition = getConditionWithParameters(type, threshold);

        searchCountShouldReturn(threshold + 1);
        // AlertCondition was never triggered before
        alertLastTriggered(-1);
        final AlertCondition.CheckResult result = alertService.triggered(messageCountAlertCondition);

        assertFalse("We should not be in grace period!", alertService.inGracePeriod(messageCountAlertCondition));
        assertTriggered(messageCountAlertCondition, result);
    }

    @Test
    public void testRunCheckLessPositive() throws Exception {
        final MessageCountAlertCondition.ThresholdType type = MessageCountAlertCondition.ThresholdType.LESS;

        final MessageCountAlertCondition messageCountAlertCondition = getConditionWithParameters(type, threshold);

        searchCountShouldReturn(threshold - 1);
        alertLastTriggered(-1);

        final AlertCondition.CheckResult result = alertService.triggered(messageCountAlertCondition);

        assertTriggered(messageCountAlertCondition, result);
    }

    @Test
    public void testRunCheckMoreNegative() throws Exception {
        final MessageCountAlertCondition.ThresholdType type = MessageCountAlertCondition.ThresholdType.MORE;

        final MessageCountAlertCondition messageCountAlertCondition = getConditionWithParameters(type, threshold);

        searchCountShouldReturn(threshold);
        alertLastTriggered(-1);

        final AlertCondition.CheckResult result = alertService.triggered(messageCountAlertCondition);

        assertNotTriggered(result);
    }

    @Test
    public void testRunCheckLessNegative() throws Exception {
        final MessageCountAlertCondition.ThresholdType type = MessageCountAlertCondition.ThresholdType.LESS;

        final MessageCountAlertCondition messageCountAlertCondition = getConditionWithParameters(type, threshold);

        searchCountShouldReturn(threshold);
        alertLastTriggered(-1);

        final AlertCondition.CheckResult result = alertService.triggered(messageCountAlertCondition);

        assertNotTriggered(result);
    }

    @Test
    public void testNoRecheckDuringGracePeriod() throws Exception {
        final MessageCountAlertCondition.ThresholdType type = MessageCountAlertCondition.ThresholdType.LESS;
        final int grace = 10;
        final int time = 10;

        final MessageCountAlertCondition messageCountAlertCondition = getMessageCountAlertCondition(
            getParametersMap(grace, time, MessageCountAlertCondition.ThresholdType.MORE, threshold),
            alertConditionTitle
        );


        try {
            verify(searches, never()).count(anyString(), any(TimeRange.class), anyString());
        } catch (InvalidRangeFormatException e) {
            assertNull("This should not throw an exception", e);
        }

        alertLastTriggered(0);
        assertTrue("Alert condition should be in grace period because grace is greater than zero and alert has just been triggered!",
            alertService.inGracePeriod(messageCountAlertCondition));
        final AlertCondition.CheckResult resultJustTriggered = alertService.triggered(messageCountAlertCondition);
        assertNotTriggered(resultJustTriggered);

        alertLastTriggered(grace * 60 - 1);
        assertTrue("Alert condition should be in grace period because grace is greater than zero and alert has been triggered during grace period!",
            alertService.inGracePeriod(messageCountAlertCondition));
        final AlertCondition.CheckResult resultTriggeredAgo = alertService.triggered(messageCountAlertCondition);
        assertNotTriggered(resultTriggeredAgo);
    }

    private MessageCountAlertCondition getConditionWithParameters(MessageCountAlertCondition.ThresholdType type, Integer threshold) {
        Map<String, Object> parameters = simplestParameterMap(type, threshold);
        return getMessageCountAlertCondition(parameters, alertConditionTitle);
    }

    private Map<String, Object> simplestParameterMap(MessageCountAlertCondition.ThresholdType type, Integer threshold) {
        return getParametersMap(0, 0, type, threshold);
    }

    private void searchCountShouldReturn(long count) {
        final CountResult countResult = mock(CountResult.class);
        when(countResult.count()).thenReturn(count);

        try {
            when(searches.count(anyString(), any(TimeRange.class), anyString())).thenReturn(countResult);
        } catch (InvalidRangeFormatException e) {
            assertNotNull("This should not return an exception!", e);
        }
    }

    private MessageCountAlertCondition getMessageCountAlertCondition(Map<String, Object> parameters, String title) {
        return new MessageCountAlertCondition(
            searches,
            stream,
            CONDITION_ID,
            Tools.nowUTC(),
            STREAM_CREATOR,
            parameters,
            title);
    }

    private Map<String, Object> getParametersMap(Integer grace, Integer time, MessageCountAlertCondition.ThresholdType type, Number threshold) {
        Map<String, Object> parameters = super.getParametersMap(grace, time, threshold);
        parameters.put("threshold_type", type.toString());

        return parameters;
    }
}
