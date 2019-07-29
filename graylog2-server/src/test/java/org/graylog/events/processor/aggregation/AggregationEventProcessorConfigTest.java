package org.graylog.events.processor.aggregation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import org.graylog.events.JobSchedulerTestClock;
import org.graylog.events.fields.providers.TemplateFieldValueProvider;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.events.processor.DBEventProcessorStateService;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.events.processor.storage.PersistToStreamsStorageHandler;
import org.graylog.events.processor.EventProcessorExecutionJob;
import org.graylog.scheduler.schedule.IntervalJobSchedule;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
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
}
