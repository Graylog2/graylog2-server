/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.alerts.types;

import org.graylog2.alerts.AlertConditionTest;
import org.graylog2.indexer.IndexHelper;
import org.graylog2.indexer.results.FieldStatsResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.AlertCondition;
import org.mockito.Matchers;
import org.testng.annotations.Test;

import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@Test(enabled=false)
public class FieldValueAlertConditionTest extends AlertConditionTest {
    public void testConstructor() throws Exception {
        Map<String, Object> parameters = getParametersMap(0,
                0,
                FieldValueAlertCondition.ThresholdType.HIGHER,
                FieldValueAlertCondition.CheckType.MAX,
                0,
                "response_time");

        final FieldValueAlertCondition fieldValueAlertCondition = getTestInstance(FieldValueAlertCondition.class, parameters);

        assertNotNull(fieldValueAlertCondition);
        assertNotNull(fieldValueAlertCondition.getDescription());
    }

    public void testRunCheckHigherPositive() throws Exception {
        for (FieldValueAlertCondition.CheckType checkType : FieldValueAlertCondition.CheckType.values()) {
            final double threshold = 50.0;
            final double higherThanThreshold = threshold + 10;
            final FieldValueAlertCondition fieldValueAlertCondition = getTestInstance(FieldValueAlertCondition.class,
                    getParametersMap(0, 0, FieldValueAlertCondition.ThresholdType.HIGHER, checkType, threshold, "response_time"));

            fieldStatsShouldReturn(getFieldStatsResult(checkType, higherThanThreshold));
            alertLastTriggered(-1);

            AlertCondition.CheckResult result = alertService.triggered(fieldValueAlertCondition);

            assertTriggered(fieldValueAlertCondition, result);
        }
    }

    public void testRunCheckHigherNegative() throws Exception {
        for (FieldValueAlertCondition.CheckType checkType : FieldValueAlertCondition.CheckType.values()) {
            final double threshold = 50.0;
            final double lowerThanThreshold = threshold - 10;
            FieldValueAlertCondition fieldValueAlertCondition = getFieldValueAlertCondition(getParametersMap(0, 0,
                    FieldValueAlertCondition.ThresholdType.HIGHER,
                    checkType, threshold, "response_time"));

            fieldStatsShouldReturn(getFieldStatsResult(checkType, lowerThanThreshold));
            alertLastTriggered(-1);

            AlertCondition.CheckResult result = alertService.triggered(fieldValueAlertCondition);

            assertNotTriggered(result);
        }
    }

    public void testRunCheckLowerPositive() throws Exception {
        for (FieldValueAlertCondition.CheckType checkType : FieldValueAlertCondition.CheckType.values()) {
            final double threshold = 50.0;
            final double lowerThanThreshold = threshold - 10;
            FieldValueAlertCondition fieldValueAlertCondition = getFieldValueAlertCondition(getParametersMap(0, 0,
                    FieldValueAlertCondition.ThresholdType.LOWER,
                    checkType, threshold, "response_time"));

            fieldStatsShouldReturn(getFieldStatsResult(checkType, lowerThanThreshold));
            alertLastTriggered(-1);

            AlertCondition.CheckResult result = alertService.triggered(fieldValueAlertCondition);

            assertTriggered(fieldValueAlertCondition, result);
        }
    }

    public void testRunCheckLowerNegative() throws Exception {
        for (FieldValueAlertCondition.CheckType checkType : FieldValueAlertCondition.CheckType.values()) {
            final double threshold = 50.0;
            final double higherThanThreshold = threshold + 10;
            FieldValueAlertCondition fieldValueAlertCondition = getFieldValueAlertCondition(getParametersMap(0, 0,
                    FieldValueAlertCondition.ThresholdType.LOWER,
                    checkType, threshold, "response_time"));

            fieldStatsShouldReturn(getFieldStatsResult(checkType, higherThanThreshold));
            alertLastTriggered(-1);

            AlertCondition.CheckResult result = alertService.triggered(fieldValueAlertCondition);

            assertNotTriggered(result);
        }
    }

    protected Map<String, Object> getParametersMap(Integer grace,
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

    protected FieldValueAlertCondition getFieldValueAlertCondition(Map<String,Object> parameters) {
        return new FieldValueAlertCondition(
                searches,
                stream,
                CONDITION_ID,
                Tools.iso8601(),
                STREAM_CREATOR,
                parameters);
    }

    protected void fieldStatsShouldReturn(FieldStatsResult fieldStatsResult) {
        try {
            when(searches.fieldStats(anyString(), Matchers.eq("*"), anyString(), any(RelativeRange.class))).thenReturn(fieldStatsResult);
        } catch (IndexHelper.InvalidRangeFormatException e) {
            assertNotNull("This should not return an exception!", e);
        } catch (Searches.FieldTypeException e) {
            assertNotNull("This should not return an exception!", e);
        }
    }

    protected FieldStatsResult getFieldStatsResult(FieldValueAlertCondition.CheckType type, Number retValue) {
        final Double value = (Double)retValue;
        final FieldStatsResult fieldStatsResult = mock(FieldStatsResult.class);

        when(fieldStatsResult.getCount()).thenReturn(1L);

        switch (type) {
            case MIN:
                when(fieldStatsResult.getMin()).thenReturn(value);
            case MAX:
                when(fieldStatsResult.getMax()).thenReturn(value);
            case MEAN:
                when(fieldStatsResult.getMean()).thenReturn(value);
            case STDDEV:
                when(fieldStatsResult.getStdDeviation()).thenReturn(value);
            case SUM:
                when(fieldStatsResult.getSum()).thenReturn(value);
        }
        return fieldStatsResult;
    }
}
