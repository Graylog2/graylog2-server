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
package org.graylog.plugins.views.search;

import com.codahale.metrics.json.MetricsModule;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
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
import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog2.database.ObjectIdSerializer;
import org.graylog2.jackson.JodaTimePeriodKeyDeserializer;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.shared.jackson.SizeSerializer;
import org.graylog2.shared.rest.RangeJsonSerializer;
import org.joda.time.Period;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.ImmutableMap.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class QueryTest {

    private ObjectMapper objectMapper;

    @Before
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
    public void mergeWithExecutionState() throws Exception {
        final String messageListId = UUID.randomUUID().toString();
        Query query = Query.builder()
                .id("abc123")
                .timerange(RelativeRange.create(600))
                .query(ElasticsearchQueryString.builder().queryString("*").build())
                .searchTypes(ImmutableSet.of(MessageList.builder().id(messageListId).build()))
                .build();
        Map<String, Object> executionState = of(
                "timerange", of("type", RelativeRange.RELATIVE, "range", "60"),
                "search_types", of(
                        messageListId,
                        of("type", MessageList.NAME, "id", messageListId, "offset", 150, "limit", 300)
                )
        );

        final Query mergedQuery = query.applyExecutionState(objectMapper, objectMapper.convertValue(executionState, JsonNode.class));
        assertThat(mergedQuery)
                .isNotEqualTo(query)
                .extracting(Query::timerange).extracting("range").containsExactly(60);

        final Optional<SearchType> messageList = mergedQuery.searchTypes().stream().filter(searchType -> messageListId.equals(searchType.id())).findFirst();
        assertThat(messageList).isPresent();
        final MessageList msgList = (MessageList) messageList.get();
        assertThat(msgList).extracting(MessageList::offset).containsExactly(150);
        assertThat(msgList).extracting(MessageList::limit).containsExactly(300);
    }

    @Test
    public void appliesExecutionStateTimeRangeToGlobalOverride() {
        Map<String, Object> executionState = of(
                "timerange", of("type", RelativeRange.RELATIVE, "range", "60")
        );
        Query sut = validQueryBuilder().build();
        Query query = sut.applyExecutionState(objectMapper, objectMapper.convertValue(executionState, JsonNode.class));
        assertThat(query.globalOverride()).hasValueSatisfying(go ->
                assertThat(go.timerange()).contains(relativeRange(60)));
    }
    @Test
    public void appliesExecutionStateQueryToGlobalOverride() {
        Map<String, Object> executionState = of(
                "query", of("type", ElasticsearchQueryString.NAME, "query_string", "NACKEN")
        );
        Query sut = validQueryBuilder().build();
        Query query = sut.applyExecutionState(objectMapper, objectMapper.convertValue(executionState, JsonNode.class));
        assertThat(query.globalOverride()).hasValueSatisfying(go ->
                assertThat(go.query()).contains(ElasticsearchQueryString.builder().queryString("NACKEN").build()));
    }
    @Test
    public void appliesExecutionStateTimeRangeAndQueryToGlobalOverrideIfBothArePresent() {
        Map<String, Object> executionState = of(
                "timerange", of("type", RelativeRange.RELATIVE, "range", "60"),
                "query", of("type", ElasticsearchQueryString.NAME, "query_string", "NACKEN")
        );
        Query sut = validQueryBuilder().build();
        Query query = sut.applyExecutionState(objectMapper, objectMapper.convertValue(executionState, JsonNode.class));
        assertThat(query.globalOverride()).hasValueSatisfying(go -> {
            assertThat(go.timerange()).contains(relativeRange(60));
            assertThat(go.query()).contains(ElasticsearchQueryString.builder().queryString("NACKEN").build());
        });
    }
    @Test
    public void doesNotAddGlobalOverrideIfNeitherTimeRangeNorQueryArePresent() {
        Map<String, Object> executionState = of(
                "search_types", of(
                        "some-id",
                        of("type", MessageList.NAME, "id", "some-id", "offset", 150, "limit", 300)
                )
        );
        Query sut = validQueryBuilder().build();
        Query query = sut.applyExecutionState(objectMapper, objectMapper.convertValue(executionState, JsonNode.class));
        assertThat(query.globalOverride()).isEmpty();
    }
    private RelativeRange relativeRange(int range) {
        try {
            return RelativeRange.create(range);
        } catch (InvalidRangeParametersException e) {
            throw new RuntimeException("invalid time range", e);
        }
    }
    private Query.Builder validQueryBuilder() {
        return Query.builder().id(UUID.randomUUID().toString()).timerange(mock(TimeRange.class)).query(new BackendQuery.Fallback());
    }
}
