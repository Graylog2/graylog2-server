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
package org.graylog.plugins.views.search.db;

import com.codahale.metrics.json.MetricsModule;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableMap;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog2.database.ObjectIdSerializer;
import org.graylog2.jackson.JodaTimePeriodKeyDeserializer;
import org.graylog2.shared.jackson.SizeSerializer;
import org.graylog2.shared.rest.RangeJsonSerializer;
import org.joda.time.Period;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class InMemorySearchJobServiceTest {

    private ObjectMapper objectMapper;

    @Test
    public void setup() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final TypeFactory typeFactory = mapper.getTypeFactory().withClassLoader(this.getClass().getClassLoader());

        this.objectMapper = mapper
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .setPropertyNamingStrategy(new PropertyNamingStrategy.SnakeCaseStrategy())
                .setTypeFactory(typeFactory)
                .registerModule(new GuavaModule())
                .registerModule(new JodaModule())
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule())
                .registerModule(new MetricsModule(TimeUnit.SECONDS, TimeUnit.SECONDS, false))
                .registerModule(new SimpleModule("Graylog")
                        .addKeyDeserializer(Period.class, new JodaTimePeriodKeyDeserializer())
                        .addSerializer(new RangeJsonSerializer())
                        .addSerializer(new SizeSerializer())
                        .addSerializer(new ObjectIdSerializer()));

        // kludge because we don't have an injector in tests
         ImmutableMap<String, Class> subtypes = ImmutableMap.<String, Class>builder()
                .put(StreamFilter.NAME, StreamFilter.class)
                .put(ElasticsearchQueryString.NAME, ElasticsearchQueryString.class)
                .put(MessageList.NAME, MessageList.class)
                .build();

        subtypes.forEach((name, klass) -> objectMapper.registerSubtypes(new NamedType(klass, name)));
    }

    @Test
    public void create() {
    }

}
