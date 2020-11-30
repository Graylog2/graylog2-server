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
package org.graylog.events.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.mongodb.DBCollection;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mongojack.Id;
import org.mongojack.JacksonDBCollection;
import org.mongojack.ObjectId;

import static org.assertj.core.api.Assertions.assertThat;

public class ESMongoDateTimeDeserializerTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        objectMapper = new ObjectMapperProvider().get();
    }

    @Test
    public void deserializeDateTime() throws Exception {
        final String json = "{\"date_time\":\"2016-12-13 14:00:00.000\"}";
        final DTO value = objectMapper.readValue(json, DTO.class);
        assertThat(value.dateTime).isEqualTo(new DateTime(2016, 12, 13, 14, 0, DateTimeZone.UTC));
    }

    @Test
    public void deserializeIsoDateTime() throws Exception {
        final String json = "{\"date_time\":\"2016-12-13T14:00:00.000\"}";
        final DTO value = objectMapper.readValue(json, DTO.class);
        assertThat(value.dateTime).isEqualTo(new DateTime(2016, 12, 13, 14, 0, DateTimeZone.UTC));
    }

    @Test
    @MongoDBFixtures("DateTime.json")
    public void deserializeMongoDateTime() throws Exception {
        final DBCollection date_collection = mongodb.mongoConnection().getDatabase().getCollection("date_collection");
        final JacksonDBCollection<DTO, ObjectId> db = JacksonDBCollection.wrap(date_collection, DTO.class, ObjectId.class, objectMapper, null);

        final DTO value = db.findOne();
        assertThat(value.dateTime).isEqualTo(new DateTime(2019, 1, 13, 14, 0, DateTimeZone.UTC));
    }

    private static class DTO {
        @Id
        @ObjectId
        @JsonProperty
        String id;

        @JsonProperty
        @JsonDeserialize(using = ESMongoDateTimeDeserializer.class)
        DateTime dateTime;
    }
}
