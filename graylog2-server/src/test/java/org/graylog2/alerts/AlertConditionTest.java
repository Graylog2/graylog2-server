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
package org.graylog2.alerts;

import com.google.common.collect.Maps;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public abstract class AlertConditionTest {
    protected static final String alertConditionTitle = "Alert Condition for Testing";
    @Mock
    protected Stream stream;
    @Mock
    protected Searches searches;

    private final String STREAM_ID = "STREAMMOCKID";
    protected final String STREAM_CREATOR = "MOCKUSER";
    protected final String CONDITION_ID = "CONDITIONMOCKID";

    @BeforeEach
    public void setUp() throws Exception {
        when(stream.getId()).thenReturn(STREAM_ID);
    }

    protected void assertTriggered(AlertCondition alertCondition, AlertCondition.CheckResult result) {
        assertTrue(result.isTriggered(), "AlertCondition should be triggered, but it's not!");
        assertNotNull(result.getTriggeredAt(), "Timestamp of returned check result should not be null!");
        assertEquals(result.getTriggeredCondition(), alertCondition, "AlertCondition of result is not the same we created!");
        long difference = Tools.nowUTC().getMillis() - result.getTriggeredAt().getMillis();
        assertTrue(difference < 1000, "AlertCondition should be triggered about now");
    }

    protected void assertNotTriggered(AlertCondition.CheckResult result) {
        assertFalse(result.isTriggered(), "AlertCondition should not be triggered, but it is!");
        assertNull(result.getTriggeredAt(), "No timestamp should be supplied if condition did not trigger");
        assertNull(result.getTriggeredCondition(), "No triggered alert condition should be supplied if condition did not trigger");
    }

    protected Map<String, Object> getParametersMap(Integer grace, Integer time, Number threshold) {
        Map<String, Object> parameters = Maps.newHashMap();
        parameters.put("grace", grace);
        parameters.put("time", time);
        parameters.put("threshold", threshold);
        return parameters;
    }

    protected <T extends AbstractAlertCondition> T getTestInstance(Class<T> klazz, Map<String, Object> parameters, String title) {
        try {
            return klazz.getConstructor(Searches.class, Stream.class, String.class, DateTime.class, String.class, Map.class, String.class)
                .newInstance(searches, stream, CONDITION_ID, Tools.nowUTC(), STREAM_CREATOR, parameters, title);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
