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
package org.graylog2.alarmcallbacks;

import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.rest.models.alarmcallbacks.requests.CreateAlarmCallbackRequest;
import org.graylog2.streams.StreamImpl;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AlarmCallbackConfigurationServiceImplTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();

    @Rule
    public MongoConnectionRule mongoRule = MongoConnectionRule.build("test");

    private AlarmCallbackConfigurationService alarmCallbackConfigurationService;

    @Before
    public void setUpService() throws Exception {
        this.alarmCallbackConfigurationService = new AlarmCallbackConfigurationServiceImpl(mongoRule.getMongoConnection());
    }

    @Test
    @UsingDataSet(locations = "alarmCallbackConfigurationsSingleDocument.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testGetForStreamIdSingleDocument() throws Exception {
        final List<AlarmCallbackConfiguration> configs = alarmCallbackConfigurationService.getForStreamId("5400deadbeefdeadbeefaffe");

        assertNotNull("Returned list should not be null", configs);
        assertEquals("Returned list should contain a single document", 1, configs.size());
    }

    @Test
    @UsingDataSet(locations = {"alarmCallbackConfigurationsSingleDocument.json", "alarmCallbackConfigurationsSingleDocument2.json"}, loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testGetForStreamIdMultipleDocuments() throws Exception {
        assertEquals("There should be multiple documents in the collection", 2, alarmCallbackConfigurationService.count());
        testGetForStreamIdSingleDocument();
    }

    @Test
    @UsingDataSet(locations = "alarmCallbackConfigurationsSingleDocument.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testGetForStreamSingleDocument() throws Exception {
        final Stream stream = mock(StreamImpl.class);
        final String streamId = "5400deadbeefdeadbeefaffe";
        when(stream.getId()).thenReturn(streamId);

        final List<AlarmCallbackConfiguration> configs = alarmCallbackConfigurationService.getForStream(stream);

        assertNotNull("Returned list should not be null", configs);
        assertEquals("Returned list should contain a single document", 1, configs.size());
    }

    @Test
    @UsingDataSet(locations = {"alarmCallbackConfigurationsSingleDocument.json", "alarmCallbackConfigurationsSingleDocument2.json"}, loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testGetForStreamMultipleDocuments() throws Exception {
        assertEquals("There should be multiple documents in the collection", 2, alarmCallbackConfigurationService.count());
        testGetForStreamSingleDocument();
    }

    @Test
    @UsingDataSet(locations = {"alarmCallbackConfigurationsSingleDocument.json", "alarmCallbackConfigurationsSingleDocument2.json"}, loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testLoadExisitingDocument() throws Exception {
        final AlarmCallbackConfiguration config = alarmCallbackConfigurationService.load("54e3deadbeefdeadbeefaffe");

        assertNotNull("Returned AlarmCallback configuration should not be null", config);
        assertEquals("AlarmCallbackConfiguration should be of dummy type", "dummy.type", config.getType());
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void testLoadNonExistingDocument() throws Exception {
        final AlarmCallbackConfiguration config = alarmCallbackConfigurationService.load(new ObjectId().toHexString());

        assertNull("No AlarmCallbackConfiguration should have been returned", config);
    }

    @Test(expected = IllegalArgumentException.class)
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void testLoadInvalidObjectId() throws Exception {
        final AlarmCallbackConfiguration config = alarmCallbackConfigurationService.load("foobar");
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void testCreate() throws Exception {
        final CreateAlarmCallbackRequest request = new CreateAlarmCallbackRequest();
        request.type = "";
        request.configuration = Collections.emptyMap();

        final String streamId = "54e3deadbeefdeadbeefaffe";
        final String userId = "someuser";

        final AlarmCallbackConfiguration alarmCallbackConfiguration = this.alarmCallbackConfigurationService.create(streamId, request, userId);

        assertNotNull(alarmCallbackConfiguration);
        assertEquals(alarmCallbackConfiguration.getStreamId(), streamId);
        assertEquals("Create should not save the object", 0, alarmCallbackConfigurationService.count());
    }
}