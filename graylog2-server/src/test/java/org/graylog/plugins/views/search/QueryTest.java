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
import com.fasterxml.jackson.core.JsonProcessingException;
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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog.plugins.views.search.rest.ExecutionStateGlobalOverride;
import org.graylog.plugins.views.search.rest.SearchTypeExecutionState;
import org.graylog.plugins.views.search.searchfilters.model.InlineQueryStringSearchFilter;
import org.graylog.plugins.views.search.searchfilters.model.ReferencedQueryStringSearchFilter;
import org.graylog.plugins.views.search.searchfilters.model.UsedSearchFilter;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog2.contentpacks.EntityDescriptorIds;
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

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
                .query(ElasticsearchQueryString.of("*"))
                .searchTypes(ImmutableSet.of(MessageList.builder().id(messageListId).build()))
                .build();


        ExecutionStateGlobalOverride.Builder executionState = ExecutionStateGlobalOverride.builder();

        executionState.timerange(RelativeRange.create(60));
        executionState.searchTypesBuilder().put(messageListId,  SearchTypeExecutionState.builder().offset(150).limit(300).build());

        final Query mergedQuery = query.applyExecutionState(executionState.build());
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
    public void appliesExecutionStateTimeRangeToGlobalOverride() throws InvalidRangeParametersException {


        final ExecutionStateGlobalOverride executionState = ExecutionStateGlobalOverride.builder()
                .timerange(RelativeRange.create(60))
                .build();


        Query sut = validQueryBuilder().build();
        Query query = sut.applyExecutionState(executionState);
        assertThat(query.globalOverride()).hasValueSatisfying(go ->
                assertThat(go.timerange()).contains(relativeRange(60)));
    }
    @Test
    public void appliesExecutionStateQueryToGlobalOverride() {

        final ExecutionStateGlobalOverride executionState = ExecutionStateGlobalOverride.builder()
                .query(ElasticsearchQueryString.of("NACKEN"))
                .build();

        Query sut = validQueryBuilder().build();
        Query query = sut.applyExecutionState(executionState);
        assertThat(query.globalOverride()).hasValueSatisfying(go ->
                assertThat(go.query()).contains(ElasticsearchQueryString.of("NACKEN")));
    }
    @Test
    public void appliesExecutionStateTimeRangeAndQueryToGlobalOverrideIfBothArePresent() throws InvalidRangeParametersException {

        final ExecutionStateGlobalOverride executionState = ExecutionStateGlobalOverride.builder()
                .timerange(RelativeRange.create(60))
                .query(ElasticsearchQueryString.of("NACKEN"))
                .build();


        Query sut = validQueryBuilder().build();
        Query query = sut.applyExecutionState(executionState);
        assertThat(query.globalOverride()).hasValueSatisfying(go -> {
            assertThat(go.timerange()).contains(relativeRange(60));
            assertThat(go.query()).contains(ElasticsearchQueryString.of("NACKEN"));
        });
    }
    @Test
    public void doesNotAddGlobalOverrideIfNeitherTimeRangeNorQueryArePresent() {

        final ExecutionStateGlobalOverride.Builder executionState = ExecutionStateGlobalOverride.builder();
        executionState.searchTypesBuilder().put("some-id",
                SearchTypeExecutionState.builder().offset(150).limit(300).build());

        Query sut = validQueryBuilder().build();
        Query query = sut.applyExecutionState(executionState.build());
        assertThat(query.globalOverride()).isEmpty();
    }

    @Test
    public void builderGeneratesQueryId() {
        final Query build = Query.builder().timerange(mock(TimeRange.class)).query(ElasticsearchQueryString.empty()).build();
        assertThat(build.id()).isNotNull();
    }

    @Test
    public void builderGeneratesDefaultQueryAndRange() {
        final Query build = Query.builder().build();
        final BackendQuery query = build.query();
        assertThat(query.queryString()).isEqualTo("");
        assertThat(build.timerange()).isNotNull();
    }

    private RelativeRange relativeRange(int range) {
        try {
            return RelativeRange.create(range);
        } catch (InvalidRangeParametersException e) {
            throw new RuntimeException("invalid time range", e);
        }
    }
    private Query.Builder validQueryBuilder() {
        return Query.builder().id(UUID.randomUUID().toString()).timerange(mock(TimeRange.class)).query(ElasticsearchQueryString.empty());
    }

    /**
     * Test that json parser recognizes full query with its type and query string value as an object (backwards compatibility)
     */
    @Test
    public void testFullQueryWithType() throws IOException {
        final Query query = objectMapper.readValue(getClass().getResourceAsStream("/org/graylog/plugins/views/search/query/full-query.json"), Query.class);
        final ElasticsearchQueryString queryString = (ElasticsearchQueryString) query.query();
        assertThat(queryString.queryString()).isEqualTo("some-full-query");
    }

    /**
     * Test that json parser recognizes query that's just a string, not object
     */
    @Test
    public void testSimpleQuery() throws IOException {
        final Query query = objectMapper.readValue(getClass().getResourceAsStream("/org/graylog/plugins/views/search/query/simple-query.json"), Query.class);
        final ElasticsearchQueryString queryString = (ElasticsearchQueryString) query.query();
        assertThat(queryString.queryString()).isEqualTo("some-simple-query");
    }

    @Test
    public void testSerializeQuery() throws JsonProcessingException {
        final String value = objectMapper.writeValueAsString(ElasticsearchQueryString.of("foo:bar"));
        assertThat(value).isEqualTo("{\"type\":\"elasticsearch\",\"query_string\":\"foo:bar\"}");
    }

    @Test
    public void testHasReferencedSearchFiltersReturnsFalseOnEmptySearchFilters() {
        Query query = Query.builder()
                .filters(Collections.emptyList())
                .build();

        assertThat(query.hasReferencedStreamFilters())
                .isFalse();
    }

    @Test
    public void testHasReferencedSearchFiltersReturnsFalseWhenNoReferencedSearchFilters() {
        Query query = Query.builder()
                .filters(Collections.singletonList(InlineQueryStringSearchFilter.builder().title("title").description("descr").queryString("*").build()))
                .build();

        assertThat(query.hasReferencedStreamFilters())
                .isFalse();
    }

    @Test
    public void testHasReferencedSearchFiltersReturnsTrueWhenReferencedSearchFilterPresent() {
        Query query = Query.builder()
                .filters(ImmutableList.of(
                        InlineQueryStringSearchFilter.builder().title("title").description("descr").queryString("*").build(),
                        ReferencedQueryStringSearchFilter.create("007")))
                .build();

        assertThat(query.hasReferencedStreamFilters())
                .isTrue();
    }

    @Test
    public void testSavesEmptySearchFiltersCollectionInContentPack() {
        Query noFiltersQuery = Query.builder().build();
        assertThat(noFiltersQuery.toContentPackEntity(EntityDescriptorIds.empty()).filters())
                .isNotNull()
                .isEmpty();
    }

    @Test
    public void testSavesSearchFiltersCollectionInContentPack() {
        final ImmutableList<UsedSearchFilter> originalSearchFilters = ImmutableList.of(
                InlineQueryStringSearchFilter.builder().title("title").description("descr").queryString("*").build(),
                ReferencedQueryStringSearchFilter.create("007")
        );
        Query queryWithFilters = Query.builder().filters(originalSearchFilters).build();
        assertThat(queryWithFilters.toContentPackEntity(EntityDescriptorIds.empty()).filters())
                .isNotNull()
                .isEqualTo(originalSearchFilters);
    }
}
