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
package org.graylog2.inputs.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import org.bson.types.ObjectId;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.rest.models.system.inputs.responses.InputDeleted;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mongojack.JacksonDBCollection;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;


public class InputStatusServiceTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    // Code Under Test
    private InputStatusService cut;

    @Mock
    EventBus mockEventBus;

    private JacksonDBCollection<InputStatusRecord, ObjectId> db;

    @Before
    public void setUp() {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final MongoJackObjectMapperProvider mapperProvider = new MongoJackObjectMapperProvider(objectMapper);

        cut = new InputStatusService(mongodb.mongoConnection(), mapperProvider, mockEventBus);

        db = JacksonDBCollection.wrap(mongodb.mongoConnection().getDatabase().getCollection(InputStatusService.COLLECTION_NAME),
                InputStatusRecord.class,
                ObjectId.class,
                mapperProvider.get());
    }

    @Test
    @MongoDBFixtures("input-status.json")
    public void get_ReturnsRecord_WhenRecordPresentInDB() {
        Optional<InputStatusRecord> optDbRecord = cut.get("54e3deadbeefdeadbeef0001");

        assertThat(optDbRecord, notNullValue());
        assertThat(optDbRecord.isPresent(), is(true));
        assertThat(optDbRecord.get(), instanceOf(InputStatusRecord.class));

        InputStatusRecord record = optDbRecord.get();

        assertThat(record.inputId(), is("54e3deadbeefdeadbeef0001"));
        assertThat(record.inputStateData(), instanceOf(InputStateData.class));
        assertThat(record.inputStateData().type(), is("test_type_1"));

        optDbRecord = cut.get("54e3deadbeefdeadbeef0002");

        assertThat(optDbRecord, notNullValue());
        assertThat(optDbRecord.isPresent(), is(true));
        assertThat(optDbRecord.get(), instanceOf(InputStatusRecord.class));

        record = optDbRecord.get();

        assertThat(record.inputId(), is("54e3deadbeefdeadbeef0002"));
        assertThat(record.inputStateData(), instanceOf(InputStateData.class));
        assertThat(record.inputStateData().type(), is("test_type_2"));

        optDbRecord = cut.get("54e3deadbeefdeadbeef0003");

        assertThat(optDbRecord, notNullValue());
        assertThat(optDbRecord.isPresent(), is(true));
        assertThat(optDbRecord.get(), instanceOf(InputStatusRecord.class));

        record = optDbRecord.get();

        assertThat(record.inputId(), is("54e3deadbeefdeadbeef0003"));
        assertThat(record.inputStateData(), instanceOf(InputStateData.class));
        assertThat(record.inputStateData().type(), is("test_type_5"));
    }

    @Test
    @MongoDBFixtures("input-status.json")
    public void get_ReturnsEmptyOptional_WhenNoRecordInDB() {
        Optional<InputStatusRecord> optDbRecord = cut.get("54e3deadbeefdeadbeef9999");

        assertThat(optDbRecord, notNullValue());
        assertThat(optDbRecord.isPresent(), is(false));
    }

    @Test
    @MongoDBFixtures("input-status.json")
    public void get_ReturnsRecord_OnlyAfterRecordSaved() {
        Optional<InputStatusRecord> optDbRecord = cut.get("54e3deadbeefdeadbeef8888");

        assertThat(optDbRecord, notNullValue());
        assertThat(optDbRecord.isPresent(), is(false));

        InputStatusRecord savedRecord = cut.save(InputStatusRecord.builder()
                .inputId("54e3deadbeefdeadbeef8888")
                .inputStateData(new InputStateData() {
                    public String type() {
                        return "test_type_8888";
                    }
                }).build());
        assertThat(savedRecord.inputId(), is("54e3deadbeefdeadbeef8888"));
        assertThat(savedRecord.inputStateData(), instanceOf(InputStateData.class));
        assertThat(savedRecord.inputStateData().type(), is("test_type_8888"));

        optDbRecord = cut.get("54e3deadbeefdeadbeef8888");

        assertThat(optDbRecord, notNullValue());
        assertThat(optDbRecord.isPresent(), is(true));

        assertThat(optDbRecord.get(), instanceOf(InputStatusRecord.class));

        InputStatusRecord record = optDbRecord.get();

        assertThat(record.inputId(), is("54e3deadbeefdeadbeef8888"));
        assertThat(record.inputStateData(), instanceOf(InputStateData.class));
        assertThat(record.inputStateData().type(), is("test_type_8888"));
    }

    @Test
    @MongoDBFixtures("input-status.json")
    public void get_ReturnsEmptyOptional_OnlyAfterRecordDeleted() {
        Optional<InputStatusRecord> optDbRecord = cut.get("54e3deadbeefdeadbeef0001");

        assertThat(optDbRecord, notNullValue());
        assertThat(optDbRecord.isPresent(), is(true));
        assertThat(optDbRecord.get(), instanceOf(InputStatusRecord.class));

        int deleteCount = cut.delete("54e3deadbeefdeadbeef0001");
        assertThat(deleteCount, is(1));

        optDbRecord = cut.get("54e3deadbeefdeadbeef0001");

        assertThat(optDbRecord, notNullValue());
        assertThat(optDbRecord.isPresent(), is(false));
    }

    @Test
    @MongoDBFixtures("input-status.json")
    public void delete_ReturnsZero_WhenDeletingNonExistantRecord() {
        Optional<InputStatusRecord> optDbRecord = cut.get("54e3deadbeefdeadbeef7777");

        assertThat(optDbRecord, notNullValue());
        assertThat(optDbRecord.isPresent(), is(false));

        int deleteCount = cut.delete("54e3deadbeefdeadbeef7777");
        assertThat(deleteCount, is(0));
    }

    @Test
    @MongoDBFixtures("input-status.json")
    public void handleDeleteEvent_DoesNothing() {
        /*
        Currently, the InputDeleted event is propagated both when an input is stopped and when an input is deleted. We
        would like to clean up the DB when an input is deleted, but not when it is stopped.  This method is in place to
        be used once there are separate events for input deleted and input stopped.  For now, it should do nothing.
         */

        cut.handleInputDeleted(new InputDeleted(){
            @Override
            public String id() {
                return "54e3deadbeefdeadbeef0001";
            }
        });

        // The record should not be removed from the DB
        Optional<InputStatusRecord> optDbRecord = cut.get("54e3deadbeefdeadbeef0001");

        assertThat(optDbRecord, notNullValue());
        assertThat(optDbRecord.isPresent(), is(true));
    }
}
