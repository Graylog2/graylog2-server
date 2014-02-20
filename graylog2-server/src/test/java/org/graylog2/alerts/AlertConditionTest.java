/*
 * Copyright 2014 TORCH GmbH
 *
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

package org.graylog2.alerts;

import com.google.common.collect.Maps;
import org.graylog2.Core;
import org.graylog2.indexer.Indexer;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.BeforeClass;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.*;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@PrepareForTest(Alert.class)
public class AlertConditionTest extends PowerMockTestCase {
    protected Stream stream;
    protected Core core;
    protected Indexer indexer;
    protected Searches searches;

    protected final String STREAM_ID = "STREAMMOCKID";
    protected final String STREAM_CREATOR = "MOCKUSER";
    protected final String CONDITION_ID = "CONDITIONMOCKID";

    @BeforeClass
    public void setUp() throws Exception {
        stream = mock(Stream.class);
        core = mock(Core.class);
        indexer = mock(Indexer.class);
        searches = mock(Searches.class);
        when(stream.getId()).thenReturn(STREAM_ID);
        when(indexer.searches()).thenReturn(searches);
        when(core.getIndexer()).thenReturn(indexer);
    }

    protected void assertTriggered(AlertCondition alertCondition, AlertCondition.CheckResult result) {
        assertTrue("AlertCondition should be triggered, but it's not!", result.isTriggered());
        assertNotNull("Timestamp of returned check result should not be null!", result.getTriggeredAt());
        assertEquals("AlertCondition of result is not the same we created!", result.getTriggeredCondition(), alertCondition);
        long difference = Tools.iso8601().getMillis() - result.getTriggeredAt().getMillis();
        assertTrue("AlertCondition should be triggered about now", difference < 50);
        assertFalse("Alert was triggered, so we should not be in grace period!", alertCondition.inGracePeriod());
    }

    protected void assertNotTriggered(AlertCondition.CheckResult result) {
        assertFalse("AlertCondition should not be triggered, but it is!", result.isTriggered());
        assertNull("No timestamp should be supplied if condition did not trigger", result.getTriggeredAt());
        assertNull("No triggered alert condition should be supplied if condition did not trigger", result.getTriggeredCondition());
    }

    protected Map<String, Object> getParametersMap(Integer grace, Integer time, Number threshold) {
        Map<String, Object> parameters = Maps.newHashMap();
        parameters.put("grace", grace);
        parameters.put("time", time);
        parameters.put("threshold", threshold);
        return parameters;
    }

    protected void alertLastTriggered(int seconds) {
        PowerMockito.mockStatic(Alert.class);
        PowerMockito.when(Alert.triggeredSecondsAgo(STREAM_ID, CONDITION_ID, core)).thenReturn(seconds);
    }

    protected <T extends AlertCondition> T getTestInstance(Class<T> klazz, Map<String, Object> parameters) {
        try {
            return klazz.getConstructor(Core.class, Stream.class, String.class, DateTime.class, String.class, Map.class)
                    .newInstance(core, stream, CONDITION_ID, Tools.iso8601(), STREAM_CREATOR, parameters);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
