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
package org.graylog.events.processor.aggregation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.graylog.events.JobSchedulerTestClock;
import org.graylog.events.TestEventProcessorConfig;
import org.graylog.events.conditions.Expr;
import org.graylog.events.conditions.Expression;
import org.graylog.events.fields.providers.TemplateFieldValueProvider;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.events.processor.DBEventProcessorStateService;
import org.graylog.events.processor.EventDefinitionConfiguration;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.events.processor.EventProcessorConfig;
import org.graylog.events.processor.EventProcessorExecutionJob;
import org.graylog.events.processor.storage.PersistToStreamsStorageHandler;
import org.graylog.plugins.views.search.searchfilters.db.IgnoreSearchFilters;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Average;
import org.graylog.scheduler.schedule.IntervalJobSchedule;
import org.graylog.security.entities.EntityOwnershipService;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.rest.ValidationResult;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class AggregationEventProcessorConfigTest {
    public static final EventDefinitionConfiguration EVENT_DEFINITION_CONFIGURATION = new EventDefinitionConfiguration();
    public static final int MAX_EVENT_LIMIT = EVENT_DEFINITION_CONFIGURATION.getMaxEventLimit();
    public static final TestEventProcessorConfig TEST_EVENT_CONFIG = TestEventProcessorConfig.builder()
            .message("")
            .searchWithinMs(1)
            .executeEveryMs(1).build();
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private DBEventProcessorStateService stateService;

    private DBEventDefinitionService dbService;
    private JobSchedulerTestClock clock;

    @Before
    public void setUp() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        objectMapper.registerSubtypes(new NamedType(AggregationEventProcessorConfig.class, AggregationEventProcessorConfig.TYPE_NAME));
        objectMapper.registerSubtypes(new NamedType(TemplateFieldValueProvider.Config.class, TemplateFieldValueProvider.Config.TYPE_NAME));
        objectMapper.registerSubtypes(new NamedType(PersistToStreamsStorageHandler.Config.class, PersistToStreamsStorageHandler.Config.TYPE_NAME));

        final MongoJackObjectMapperProvider mapperProvider = new MongoJackObjectMapperProvider(objectMapper);
        this.dbService = new DBEventDefinitionService(mongodb.mongoConnection(), mapperProvider, stateService,
                mock(EntityOwnershipService.class), null, new IgnoreSearchFilters());
        this.clock = new JobSchedulerTestClock(DateTime.now(DateTimeZone.UTC));
    }

    @Test
    @MongoDBFixtures("aggregation-processors.json")
    public void toJobSchedulerConfig() {
        final EventDefinitionDto dto = dbService.get("54e3deadbeefdeadbeefaffe").orElse(null);

        assertThat(dto).isNotNull();

        assertThat(dto.config().toJobSchedulerConfig(dto, clock)).isPresent().get().satisfies(schedulerConfig -> {
            assertThat(schedulerConfig.jobDefinitionConfig()).satisfies(jobDefinitionConfig -> {
                assertThat(jobDefinitionConfig).isInstanceOf(EventProcessorExecutionJob.Config.class);

                final EventProcessorExecutionJob.Config config = (EventProcessorExecutionJob.Config) jobDefinitionConfig;

                assertThat(config.eventDefinitionId()).isEqualTo(dto.id());
                assertThat(config.processingWindowSize()).isEqualTo(300000);
                assertThat(config.processingHopSize()).isEqualTo(300000);
                assertThat(config.parameters()).isEqualTo(AggregationEventProcessorParameters.builder()
                        .timerange(AbsoluteRange.create(clock.nowUTC().minus(300000), clock.nowUTC()))
                        .build());
            });

            assertThat(schedulerConfig.schedule()).satisfies(schedule -> {
                assertThat(schedule).isInstanceOf(IntervalJobSchedule.class);

                final IntervalJobSchedule config = (IntervalJobSchedule) schedule;

                assertThat(config.interval()).isEqualTo(300000);
                assertThat(config.unit()).isEqualTo(TimeUnit.MILLISECONDS);
            });
        });
    }

    private AggregationEventProcessorConfig getConfig() {
        return AggregationEventProcessorConfig.builder()
                .query("")
                .streams(new HashSet<>())
                .groupBy(new ArrayList<>())
                .series(new ArrayList<>())
                .searchWithinMs(1)
                .executeEveryMs(1)
                .eventLimit(1)
                .build();
    }

    private AggregationConditions getConditions() {
        final Expression<Boolean> expression = Expr.Greater.create(Expr.NumberReference.create("foo"),
                Expr.NumberValue.create(42.0));
        return AggregationConditions.builder()
                .expression(expression)
                .build();
    }

    private SeriesSpec getSeries() {
        return Average.builder()
                .id("123")
                .field("foo")
                .build();
    }

    @Test
    public void testValidateWithInvalidTimeRange() {
        final AggregationEventProcessorConfig invalidConfig1 = getConfig().toBuilder()
                .searchWithinMs(-1)
                .build();

        final ValidationResult validationResult1 = invalidConfig1.validate();
        assertThat(validationResult1.failed()).isTrue();
        assertThat(validationResult1.getErrors()).containsOnlyKeys("search_within_ms");

        final AggregationEventProcessorConfig invalidConfig2 = invalidConfig1.toBuilder()
                .searchWithinMs(0)
                .build();

        final ValidationResult validationResult2 = invalidConfig2.validate();
        assertThat(validationResult2.failed()).isTrue();
        assertThat(validationResult2.getErrors()).containsOnlyKeys("search_within_ms");
    }

    @Test
    public void testValidateWithInvalidExecutionTime() {
        final AggregationEventProcessorConfig invalidConfig1 = getConfig().toBuilder()
                .executeEveryMs(-1)
                .build();

        final ValidationResult validationResult1 = invalidConfig1.validate();
        assertThat(validationResult1.failed()).isTrue();
        assertThat(validationResult1.getErrors()).containsOnlyKeys("execute_every_ms");

        final AggregationEventProcessorConfig invalidConfig2 = invalidConfig1.toBuilder()
                .executeEveryMs(0)
                .build();

        final ValidationResult validationResult2 = invalidConfig2.validate();
        assertThat(validationResult2.failed()).isTrue();
        assertThat(validationResult2.getErrors()).containsOnlyKeys("execute_every_ms");
    }

    @Test
    public void testEventLimitValidation() {
        var eventLimitZero = configWithEventLimit(0);
        var eventLimitOne = configWithEventLimit(1);
        var eventLimitGreaterMax = configWithEventLimit(MAX_EVENT_LIMIT + 1);

        assertValidationError("greater than 0", eventLimitZero, null);
        assertValidationError("greater than 0", configWithEventLimit(-1), null);
        assertValidationError("greater than 0", eventLimitZero, eventLimitOne);
        assertValidationError("greater than 0", eventLimitZero, TEST_EVENT_CONFIG);
        assertValidationError("less than " + MAX_EVENT_LIMIT, eventLimitGreaterMax, eventLimitZero);
        assertNoValidationErrors(eventLimitZero, eventLimitZero);
        assertNoValidationErrors(eventLimitOne, eventLimitZero);
        assertNoValidationErrors(eventLimitOne, TEST_EVENT_CONFIG);
    }

    private AggregationEventProcessorConfig configWithEventLimit(int eventLimit) {
        return getConfig().toBuilder().eventLimit(eventLimit).build();
    }

    private static void assertValidationError(String validationError, AggregationEventProcessorConfig config,
                                              EventProcessorConfig oldConfig) {
        List<String> validationErrors = config.validate(oldConfig, EVENT_DEFINITION_CONFIGURATION).getErrors().values()
                .stream().flatMap(Collection::stream).toList();
        assertThat(validationErrors).hasSize(1);
        assertThat(validationErrors).allMatch(strings -> strings.contains(validationError));
    }

    private static void assertNoValidationErrors(AggregationEventProcessorConfig config,
                                                 EventProcessorConfig oldConfig) {
        List<String> validationErrors = config.validate(oldConfig, EVENT_DEFINITION_CONFIGURATION).getErrors().values()
                .stream().flatMap(Collection::stream).toList();
        assertThat(validationErrors).isEmpty();
    }

    @Test
    public void testEventLimitDefaultValue() {
        final AggregationEventProcessorConfig config = AggregationEventProcessorConfig.builder()
                .query("")
                .streams(new HashSet<>())
                .groupBy(new ArrayList<>())
                .series(new ArrayList<>())
                .searchWithinMs(1)
                .executeEveryMs(1)
                .build();

        assertThat(config.eventLimit()).isEqualTo(0);
    }

    @Test
    public void testValidateWithIncompleteAggregationOptions() {
        AggregationEventProcessorConfig invalidConfig = getConfig().toBuilder()
                .groupBy(ImmutableList.of("foo"))
                .build();

        ValidationResult validationResult = invalidConfig.validate();
        assertThat(validationResult.failed()).isTrue();
        assertThat(validationResult.getErrors()).containsOnlyKeys("series", "conditions");

        invalidConfig = getConfig().toBuilder()
                .series(ImmutableList.of(this.getSeries()))
                .build();

        validationResult = invalidConfig.validate();
        assertThat(validationResult.failed()).isTrue();
        assertThat(validationResult.getErrors()).containsOnlyKeys("conditions");

        invalidConfig = getConfig().toBuilder()
                .conditions(this.getConditions())
                .build();

        validationResult = invalidConfig.validate();
        assertThat(validationResult.failed()).isTrue();
        assertThat(validationResult.getErrors()).containsOnlyKeys("series");
    }

    @Test
    public void testValidConfiguration() {
        final ValidationResult validationResult = getConfig().validate();
        assertThat(validationResult.failed()).isFalse();
        assertThat(validationResult.getErrors().size()).isEqualTo(0);
    }

    @Test
    public void testValidFilterConfiguration() {
        final AggregationEventProcessorConfig config = getConfig().toBuilder()
                .query("foo")
                .streams(ImmutableSet.of("1", "2"))
                .build();

        final ValidationResult validationResult = config.validate();
        assertThat(validationResult.failed()).isFalse();
        assertThat(validationResult.getErrors().size()).isEqualTo(0);
    }

    @Test
    public void testValidAggregationConfiguration() {
        final AggregationEventProcessorConfig config = getConfig().toBuilder()
                .groupBy(ImmutableList.of("bar"))
                .series(ImmutableList.of(this.getSeries()))
                .conditions(this.getConditions())
                .build();

        final ValidationResult validationResult = config.validate();
        assertThat(validationResult.failed()).isFalse();
        assertThat(validationResult.getErrors().size()).isEqualTo(0);
    }

    @Test
    @MongoDBFixtures("aggregation-processors.json")
    public void requiredPermissions() {
        assertThat(dbService.get("54e3deadbeefdeadbeefaffe")).get().satisfies(definition ->
                assertThat(definition.config().requiredPermissions()).containsOnly("streams:read:stream-a", "streams:read:stream-b"));
    }

    @Test
    @MongoDBFixtures("aggregation-processors.json")
    public void requiredPermissionsWithEmptyStreams() {
        assertThat(dbService.get("54e3deadbeefdeadbeefafff")).get().satisfies(definition ->
                assertThat(definition.config().requiredPermissions()).containsOnly("streams:read"));
    }
}
