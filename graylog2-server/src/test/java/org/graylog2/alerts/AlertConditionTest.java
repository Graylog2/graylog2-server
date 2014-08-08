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
package org.graylog2.alerts;

import com.google.common.collect.Maps;
import com.google.inject.assistedinject.Assisted;
import org.graylog2.alerts.types.FieldValueAlertCondition;
import org.graylog2.alerts.types.MessageCountAlertCondition;
import org.graylog2.database.MongoConnection;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;
import org.testng.annotations.BeforeClass;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.*;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class AlertConditionTest {
    protected Stream stream;
    protected Searches searches;
    protected AlertService alertService;
    protected MongoConnection mongoConnection;

    protected final String STREAM_ID = "STREAMMOCKID";
    protected final String STREAM_CREATOR = "MOCKUSER";
    protected final String CONDITION_ID = "CONDITIONMOCKID";

    @BeforeClass
    public void setUp() throws Exception {
        stream = mock(Stream.class);
        searches = mock(Searches.class);
        mongoConnection = mock(MongoConnection.class);
        // TODO use injection please. this sucks so bad
        alertService = new AlertServiceImpl(mongoConnection,
                                            new FieldValueAlertCondition.Factory() {
                                                @Override
                                                public FieldValueAlertCondition createAlertCondition(Stream stream,
                                                                                                     String id,
                                                                                                     DateTime createdAt,
                                                                                                     @Assisted("userid") String creatorUserId,
                                                                                                     Map<String, Object> parameters) {
                                                    return new FieldValueAlertCondition(searches, stream, id, createdAt, creatorUserId, parameters);
                                                }
                                            },
                                            new MessageCountAlertCondition.Factory() {
                                                @Override
                                                public MessageCountAlertCondition createAlertCondition(Stream stream,
                                                                                                       String id,
                                                                                                       DateTime createdAt,
                                                                                                       @Assisted("userid") String creatorUserId,
                                                                                                       Map<String, Object> parameters) {
                                                    return new MessageCountAlertCondition(searches, stream, id, createdAt, creatorUserId, parameters);
                                                }
                                            });

        when(stream.getId()).thenReturn(STREAM_ID);
    }

    protected void assertTriggered(AlertCondition alertCondition, AlertCondition.CheckResult result) {
        assertTrue("AlertCondition should be triggered, but it's not!", result.isTriggered());
        assertNotNull("Timestamp of returned check result should not be null!", result.getTriggeredAt());
        assertEquals("AlertCondition of result is not the same we created!", result.getTriggeredCondition(), alertCondition);
        long difference = Tools.iso8601().getMillis() - result.getTriggeredAt().getMillis();
        assertTrue("AlertCondition should be triggered about now", difference < 50);
        assertFalse("Alert was triggered, so we should not be in grace period!", alertService.inGracePeriod(alertCondition));
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
        //PowerMockito.mockStatic(AlertImpl.class);
        //PowerMockito.when(alertService.triggeredSecondsAgo(STREAM_ID, CONDITION_ID)).thenReturn(seconds);
        when(alertService.triggeredSecondsAgo(STREAM_ID, CONDITION_ID)).thenReturn(seconds);
    }

    protected <T extends AbstractAlertCondition> T getTestInstance(Class<T> klazz, Map<String, Object> parameters) {
        try {
            return klazz.getConstructor(Stream.class, String.class, DateTime.class, String.class, Map.class)
                    .newInstance(stream, CONDITION_ID, Tools.iso8601(), STREAM_CREATOR, parameters);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
