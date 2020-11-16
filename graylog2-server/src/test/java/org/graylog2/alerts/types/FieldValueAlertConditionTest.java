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
import org.graylog2.indexer.results.FieldStatsResult;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FieldValueAlertConditionTest extends AlertConditionTest {
    private static final String alertConditionTitle = "Test Alert Condition";

    @Test
    public void testConstructor() throws Exception {
        Map<String, Object> parameters = getParametersMap(0,
            0,
            FieldValueAlertCondition.ThresholdType.HIGHER,
            FieldValueAlertCondition.CheckType.MAX,
            0,
            "response_time");

        final FieldValueAlertCondition fieldValueAlertCondition = getTestInstance(FieldValueAlertCondition.class, parameters, alertConditionTitle);

        assertNotNull(fieldValueAlertCondition);
        assertNotNull(fieldValueAlertCondition.getDescription());
    }

    @Test
    public void testRunCheckHigherPositive() throws Exception {
        for (FieldValueAlertCondition.CheckType checkType : FieldValueAlertCondition.CheckType.values()) {
            final double threshold = 50.0;
            final double higherThanThreshold = threshold + 10;
            final FieldValueAlertCondition fieldValueAlertCondition = getTestInstance(FieldValueAlertCondition.class,
                getParametersMap(0, 0, FieldValueAlertCondition.ThresholdType.HIGHER, checkType, threshold, "response_time"),
                alertConditionTitle);

            fieldStatsShouldReturn(getFieldStatsResult(checkType, higherThanThreshold));

            AlertCondition.CheckResult result = fieldValueAlertCondition.runCheck();

            assertTriggered(fieldValueAlertCondition, result);
        }
    }

    @Test
    public void testRunCheckHigherNegative() throws Exception {
        for (FieldValueAlertCondition.CheckType checkType : FieldValueAlertCondition.CheckType.values()) {
            final double threshold = 50.0;
            final double lowerThanThreshold = threshold - 10;
            FieldValueAlertCondition fieldValueAlertCondition = getFieldValueAlertCondition(getParametersMap(0, 0,
                FieldValueAlertCondition.ThresholdType.HIGHER,
                checkType, threshold, "response_time"),
                alertConditionTitle);

            fieldStatsShouldReturn(getFieldStatsResult(checkType, lowerThanThreshold));

            AlertCondition.CheckResult result = fieldValueAlertCondition.runCheck();

            assertNotTriggered(result);
        }
    }

    @Test
    public void testRunCheckLowerPositive() throws Exception {
        for (FieldValueAlertCondition.CheckType checkType : FieldValueAlertCondition.CheckType.values()) {
            final double threshold = 50.0;
            final double lowerThanThreshold = threshold - 10;
            FieldValueAlertCondition fieldValueAlertCondition = getFieldValueAlertCondition(getParametersMap(0, 0,
                FieldValueAlertCondition.ThresholdType.LOWER,
                checkType, threshold, "response_time"),
                alertConditionTitle);

            fieldStatsShouldReturn(getFieldStatsResult(checkType, lowerThanThreshold));

            AlertCondition.CheckResult result = fieldValueAlertCondition.runCheck();

            assertTriggered(fieldValueAlertCondition, result);
        }
    }

    @Test
    public void testRunCheckLowerNegative() throws Exception {
        for (FieldValueAlertCondition.CheckType checkType : FieldValueAlertCondition.CheckType.values()) {
            final double threshold = 50.0;
            final double higherThanThreshold = threshold + 10;
            FieldValueAlertCondition fieldValueAlertCondition = getFieldValueAlertCondition(getParametersMap(0, 0,
                FieldValueAlertCondition.ThresholdType.LOWER,
                checkType, threshold, "response_time"),
                alertConditionTitle);

            fieldStatsShouldReturn(getFieldStatsResult(checkType, higherThanThreshold));

            AlertCondition.CheckResult result = fieldValueAlertCondition.runCheck();

            assertNotTriggered(result);
        }
    }

    private Map<String, Object> getParametersMap(Integer grace,
                                                 Integer time,
                                                 FieldValueAlertCondition.ThresholdType threshold_type,
                                                 FieldValueAlertCondition.CheckType check_type,
                                                 Number threshold,
                                                 String field) {
        Map<String, Object> parameters = super.getParametersMap(grace, time, threshold);
        parameters.put("threshold_type", threshold_type.toString());
        parameters.put("field", field);
        parameters.put("type", check_type.toString());

        return parameters;
    }

    private FieldValueAlertCondition getFieldValueAlertCondition(Map<String, Object> parameters, String title) {
        return new FieldValueAlertCondition(
            searches,
            stream,
            CONDITION_ID,
            Tools.nowUTC(),
            STREAM_CREATOR,
            parameters,
            title);
    }

    private void fieldStatsShouldReturn(FieldStatsResult fieldStatsResult) {
        when(searches.fieldStats(anyString(),
                eq("*"),
                anyString(),
                any(RelativeRange.class),
                anyBoolean(),
                anyBoolean(),
                anyBoolean())).thenReturn(fieldStatsResult);
    }

    private FieldStatsResult getFieldStatsResult(FieldValueAlertCondition.CheckType type, Number retValue) {
        final Double value = (Double) retValue;
        final FieldStatsResult fieldStatsResult = mock(FieldStatsResult.class);

        when(fieldStatsResult.count()).thenReturn(1L);

        switch (type) {
            case MIN:
                when(fieldStatsResult.min()).thenReturn(value);
                break;
            case MAX:
                when(fieldStatsResult.max()).thenReturn(value);
                break;
            case MEAN:
                when(fieldStatsResult.mean()).thenReturn(value);
                break;
            case STDDEV:
                when(fieldStatsResult.stdDeviation()).thenReturn(value);
                break;
            case SUM:
                when(fieldStatsResult.sum()).thenReturn(value);
                break;
        }
        return fieldStatsResult;
    }
}
