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
package org.graylog.events.processor.aggregation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import org.graylog.events.JobSchedulerTestClock;
import org.graylog.events.conditions.Expr;
import org.graylog.events.conditions.Expression;
import org.graylog.events.fields.providers.TemplateFieldValueProvider;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.events.processor.DBEventProcessorStateService;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.events.processor.EventProcessorExecutionJob;
import org.graylog.events.processor.storage.PersistToStreamsStorageHandler;
import org.graylog.scheduler.schedule.IntervalJobSchedule;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.rest.ValidationResult;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class AggregationEventProcessorConfigTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule().build();

    @Rule
    public MongoConnectionRule mongoRule = MongoConnectionRule.build("test");
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
        this.dbService = new DBEventDefinitionService(mongoRule.getMongoConnection(), mapperProvider, stateService);
        this.clock = new JobSchedulerTestClock(DateTime.now(DateTimeZone.UTC));
    }

    @Test
    @UsingDataSet(locations = "aggregation-processors.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
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
            .build();
    }

    private AggregationConditions getConditions() {
        final Expression<Boolean> expression = Expr.Greater.create(Expr.NumberReference.create("foo"),
            Expr.NumberValue.create(42.0));
        return AggregationConditions.builder()
            .expression(expression)
            .build();
    }

    private AggregationSeries getSeries() {
        return AggregationSeries.builder()
            .id("123")
            .field("foo")
            .function(AggregationFunction.AVG)
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
}
