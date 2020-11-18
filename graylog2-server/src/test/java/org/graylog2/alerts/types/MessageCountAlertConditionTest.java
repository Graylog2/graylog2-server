/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.alerts.types;

import org.graylog2.alerts.AlertConditionTest;
import org.graylog2.indexer.results.CountResult;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.junit.Test;

import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MessageCountAlertConditionTest extends AlertConditionTest {
    private final int threshold = 100;

    @Test
    public void testConstructor() throws Exception {
        final Map<String, Object> parameters = getParametersMap(0, 0, MessageCountAlertCondition.ThresholdType.MORE, 0);

        final MessageCountAlertCondition messageCountAlertCondition = getMessageCountAlertCondition(parameters, alertConditionTitle);

        assertNotNull(messageCountAlertCondition);
        assertNotNull(messageCountAlertCondition.getDescription());
        final String thresholdType = (String) messageCountAlertCondition.getParameters().get("threshold_type");
        assertEquals(thresholdType, thresholdType.toUpperCase(Locale.ENGLISH));
    }

    /*
     * Ensure MessageCountAlertCondition objects created before 2.2.0 and having a lowercase threshold_type,
     * get converted to uppercase for consistency with new created alert conditions.
     */
    @Test
    public void testConstructorOldObjects() throws Exception {
        final Map<String, Object> parameters = getParametersMap(0, 0, MessageCountAlertCondition.ThresholdType.MORE, 0);
        parameters.put("threshold_type", MessageCountAlertCondition.ThresholdType.MORE.toString().toLowerCase(Locale.ENGLISH));

        final MessageCountAlertCondition messageCountAlertCondition = getMessageCountAlertCondition(parameters, alertConditionTitle);

        final String thresholdType = (String) messageCountAlertCondition.getParameters().get("threshold_type");
        assertEquals(thresholdType, thresholdType.toUpperCase(Locale.ENGLISH));
    }

    @Test
    public void testRunCheckMorePositive() throws Exception {
        final MessageCountAlertCondition.ThresholdType type = MessageCountAlertCondition.ThresholdType.MORE;

        final MessageCountAlertCondition messageCountAlertCondition = getConditionWithParameters(type, threshold);

        searchCountShouldReturn(threshold + 1);
        // AlertCondition was never triggered before
        final AlertCondition.CheckResult result = messageCountAlertCondition.runCheck();

        assertTriggered(messageCountAlertCondition, result);
    }

    @Test
    public void testRunCheckLessPositive() throws Exception {
        final MessageCountAlertCondition.ThresholdType type = MessageCountAlertCondition.ThresholdType.LESS;

        final MessageCountAlertCondition messageCountAlertCondition = getConditionWithParameters(type, threshold);

        searchCountShouldReturn(threshold - 1);

        final AlertCondition.CheckResult result = messageCountAlertCondition.runCheck();

        assertTriggered(messageCountAlertCondition, result);
    }

    @Test
    public void testRunCheckMoreNegative() throws Exception {
        final MessageCountAlertCondition.ThresholdType type = MessageCountAlertCondition.ThresholdType.MORE;

        final MessageCountAlertCondition messageCountAlertCondition = getConditionWithParameters(type, threshold);

        searchCountShouldReturn(threshold);

        final AlertCondition.CheckResult result = messageCountAlertCondition.runCheck();

        assertNotTriggered(result);
    }

    @Test
    public void testRunCheckLessNegative() throws Exception {
        final MessageCountAlertCondition.ThresholdType type = MessageCountAlertCondition.ThresholdType.LESS;

        final MessageCountAlertCondition messageCountAlertCondition = getConditionWithParameters(type, threshold);

        searchCountShouldReturn(threshold);

        final AlertCondition.CheckResult result = messageCountAlertCondition.runCheck();

        assertNotTriggered(result);
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

        when(searches.count(anyString(), any(TimeRange.class), anyString())).thenReturn(countResult);
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
