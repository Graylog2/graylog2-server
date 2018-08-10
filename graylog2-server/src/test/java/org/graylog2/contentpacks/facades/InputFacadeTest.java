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
package org.graylog2.contentpacks.facades;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.graph.Graph;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.EntityWithConstraints;
import org.graylog2.contentpacks.model.entities.InputEntity;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.references.ReferenceMapUtils;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.dashboards.DashboardImpl;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.grok.GrokPattern;
import org.graylog2.grok.GrokPatternService;
import org.graylog2.grok.InMemoryGrokPatternService;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputImpl;
import org.graylog2.inputs.InputService;
import org.graylog2.inputs.InputServiceImpl;
import org.graylog2.inputs.converters.ConverterFactory;
import org.graylog2.inputs.extractors.ExtractorFactory;
import org.graylog2.inputs.random.FakeHttpMessageInput;
import org.graylog2.inputs.raw.udp.RawUDPInput;
import org.graylog2.lookup.LookupTableService;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.inputs.Extractor;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.inputs.InputRegistry;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InputFacadeTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();

    @Rule
    public final MongoConnectionRule mongoRule = MongoConnectionRule.build("test");

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Mock
    private LookupTableService lookupTableService;
    @Mock
    private MessageInputFactory messageInputFactory;
    @Mock
    private ServerStatus serverStatus;

    private InputService inputService;
    private InputFacade facade;
    private Set<PluginMetaData> pluginMetaData;
    private Map<String, MessageInput.Factory<? extends MessageInput>> inputFactories;

    @Before
    @SuppressForbidden("Using Executors.newSingleThreadExecutor() is okay in tests")
    public void setUp() throws Exception {
        final MetricRegistry metricRegistry = new MetricRegistry();
        final ClusterEventBus clusterEventBus = new ClusterEventBus("cluster-event-bus", Executors.newSingleThreadExecutor());
        final GrokPatternService grokPatternService = new InMemoryGrokPatternService(clusterEventBus);
        grokPatternService.save(GrokPattern.create("GREEDY", ".*"));
        final ExtractorFactory extractorFactory = new ExtractorFactory(metricRegistry, grokPatternService, lookupTableService);
        final ConverterFactory converterFactory = new ConverterFactory(lookupTableService);
        inputService = new InputServiceImpl(mongoRule.getMongoConnection(), extractorFactory, converterFactory, messageInputFactory, clusterEventBus);
        final InputRegistry inputRegistry = new InputRegistry();
        pluginMetaData = new HashSet<>();
        inputFactories = new HashMap<>();
        final FakeHttpMessageInput.Factory fakeHttpMessageInputFactory = mock(FakeHttpMessageInput.Factory.class);
        final FakeHttpMessageInput.Descriptor fakeHttpMessageInputDescriptor = mock(FakeHttpMessageInput.Descriptor.class);
        when(fakeHttpMessageInputFactory.getDescriptor()).thenReturn(fakeHttpMessageInputDescriptor);
        final RawUDPInput.Factory rawUDPInputFactory = mock(RawUDPInput.Factory.class);
        final RawUDPInput.Descriptor rawUDPInputDescriptor = mock(RawUDPInput.Descriptor.class);
        when(rawUDPInputFactory.getDescriptor()).thenReturn(rawUDPInputDescriptor);
        inputFactories.put("org.graylog2.inputs.random.FakeHttpMessageInput", fakeHttpMessageInputFactory);
        inputFactories.put("org.graylog2.inputs.raw.udp.RawUDPInput", rawUDPInputFactory);

        facade = new InputFacade(
                objectMapper,
                inputService,
                inputRegistry,
                messageInputFactory,
                extractorFactory,
                converterFactory,
                serverStatus,
                pluginMetaData,
                inputFactories);
    }

    @Test
    public void exportNativeEntity() {
        final ImmutableMap<String, Object> fields = ImmutableMap.of(
                MessageInput.FIELD_TITLE, "Input Title",
                MessageInput.FIELD_TYPE, "org.graylog2.inputs.raw.udp.RawUDPInput",
                MessageInput.FIELD_CONFIGURATION, Collections.emptyMap()
        );
        final InputImpl input = new InputImpl(fields);
        final ImmutableList<Extractor> extractors = ImmutableList.of();
        final InputWithExtractors inputWithExtractors = InputWithExtractors.create(input, extractors);
        final EntityWithConstraints entityWithConstraints = facade.exportNativeEntity(inputWithExtractors);
        final Entity entity = entityWithConstraints.entity();

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of(input.getId()));
        assertThat(entity.type()).isEqualTo(ModelTypes.INPUT_V1);

        final EntityV1 entityV1 = (EntityV1) entity;
        final InputEntity inputEntity = objectMapper.convertValue(entityV1.data(), InputEntity.class);
        assertThat(inputEntity.title()).isEqualTo(ValueReference.of("Input Title"));
        assertThat(inputEntity.type()).isEqualTo(ValueReference.of("org.graylog2.inputs.raw.udp.RawUDPInput"));
        assertThat(inputEntity.configuration()).isEmpty();
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/inputs.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void exportEntity() {
        final ModelId id = ModelId.of("5acc84f84b900a4ff290d9a7");
        final EntityDescriptor descriptor = EntityDescriptor.create(id, ModelTypes.INPUT_V1);
        final EntityWithConstraints entityWithConstraints = facade.exportEntity(descriptor).orElseThrow(AssertionError::new);
        final Entity entity = entityWithConstraints.entity();

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(id);
        assertThat(entity.type()).isEqualTo(ModelTypes.INPUT_V1);

        final EntityV1 entityV1 = (EntityV1) entity;
        final InputEntity inputEntity = objectMapper.convertValue(entityV1.data(), InputEntity.class);
        assertThat(inputEntity.title()).isEqualTo(ValueReference.of("Local Raw UDP"));
        assertThat(inputEntity.type()).isEqualTo(ValueReference.of("org.graylog2.inputs.raw.udp.RawUDPInput"));
        assertThat(inputEntity.global()).isEqualTo(ValueReference.of(false));
        assertThat(inputEntity.configuration())
                .containsEntry("bind_address", ValueReference.of("127.0.0.1"))
                .containsEntry("port", ValueReference.of(5555));
    }


    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    @Ignore("Doesn't work without massive amount of mocks")
    public void createNativeEntity() {
        final Map<String, Object> configuration = new HashMap<>();
        configuration.put("override_source", null);
        configuration.put("recv_buffer_size", 262144);
        configuration.put("bind_address", "127.0.0.1");
        configuration.put("port", 5555);
        configuration.put("number_worker_threads", 8);
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("5acc84f84b900a4ff290d9a7"))
                .type(ModelTypes.INPUT_V1)
                .data(objectMapper.convertValue(InputEntity.create(
                        ValueReference.of("Local Raw UDP"),
                        ReferenceMapUtils.toReferenceMap(configuration),
                        Collections.emptyMap(),
                        ValueReference.of("org.graylog2.inputs.raw.udp.RawUDPInput"),
                        ValueReference.of(false),
                        Collections.emptyList()), JsonNode.class))
                .build();

        final NativeEntity<InputWithExtractors> nativeEntity = facade.createNativeEntity(entity, Collections.emptyMap(), Collections.emptyMap(), "username");

        final InputWithExtractors inputWithExtractors = nativeEntity.entity();
        final Input savedInput = inputWithExtractors.input();
        final String savedId = savedInput.getId();
        assertThat(nativeEntity.descriptor()).isEqualTo(EntityDescriptor.create(savedId, ModelTypes.INPUT_V1));
        assertThat(savedInput.getTitle()).isEqualTo("Local Raw UDP");
        assertThat(savedInput.getType()).isEqualTo("org.graylog2.inputs.raw.udp.RawUDPInput");
        assertThat(savedInput.isGlobal()).isFalse();
        assertThat(savedInput.getContentPack()).isNull();
    }

    @Test
    public void createExcerpt() {
        final ImmutableMap<String, Object> fields = ImmutableMap.of(
                DashboardImpl.FIELD_TITLE, "Dashboard Title"
        );
        final InputImpl input = new InputImpl(fields);
        final InputWithExtractors inputWithExtractors = InputWithExtractors.create(input);
        final EntityExcerpt excerpt = facade.createExcerpt(inputWithExtractors);

        assertThat(excerpt.id()).isEqualTo(ModelId.of(input.getId()));
        assertThat(excerpt.type()).isEqualTo(ModelTypes.INPUT_V1);
        assertThat(excerpt.title()).isEqualTo(input.getTitle());
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/inputs.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void listEntityExcerpts() {
        final EntityExcerpt expectedEntityExcerpt1 = EntityExcerpt.builder()
                .id(ModelId.of("5adf25294b900a0fdb4e5365"))
                .type(ModelTypes.INPUT_V1)
                .title("Global Random HTTP")
                .build();
        final EntityExcerpt expectedEntityExcerpt2 = EntityExcerpt.builder()
                .id(ModelId.of("5acc84f84b900a4ff290d9a7"))
                .type(ModelTypes.INPUT_V1)
                .title("Local Raw UDP")
                .build();

        final Set<EntityExcerpt> entityExcerpts = facade.listEntityExcerpts();
        assertThat(entityExcerpts).containsOnly(expectedEntityExcerpt1, expectedEntityExcerpt2);
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/inputs.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void collectEntity() {
        final Optional<EntityWithConstraints> collectedEntity = facade.exportEntity(EntityDescriptor.create("5adf25294b900a0fdb4e5365", ModelTypes.INPUT_V1));
        assertThat(collectedEntity)
                .isPresent()
                .map(EntityWithConstraints::entity)
                .containsInstanceOf(EntityV1.class);

        final EntityV1 entity = (EntityV1) collectedEntity.map(EntityWithConstraints::entity).orElseThrow(AssertionError::new);
        assertThat(entity.id()).isEqualTo(ModelId.of("5adf25294b900a0fdb4e5365"));
        assertThat(entity.type()).isEqualTo(ModelTypes.INPUT_V1);
        final InputEntity inputEntity = objectMapper.convertValue(entity.data(), InputEntity.class);
        assertThat(inputEntity.title()).isEqualTo(ValueReference.of("Global Random HTTP"));
        assertThat(inputEntity.type()).isEqualTo(ValueReference.of("org.graylog2.inputs.random.FakeHttpMessageInput"));
        assertThat(inputEntity.global()).isEqualTo(ValueReference.of(true));
        assertThat(inputEntity.staticFields()).containsEntry("custom_field", ValueReference.of("foobar"));
        assertThat(inputEntity.configuration()).isNotEmpty();
        assertThat(inputEntity.extractors()).hasSize(5);
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/inputs.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void findExisting() {
        final Map<String, Object> configuration = new HashMap<>();
        configuration.put("override_source", null);
        configuration.put("recv_buffer_size", 262144);
        configuration.put("bind_address", "127.0.0.1");
        configuration.put("port", 5555);
        configuration.put("number_worker_threads", 8);
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("5acc84f84b900a4ff290d9a7"))
                .type(ModelTypes.INPUT_V1)
                .data(objectMapper.convertValue(InputEntity.create(
                        ValueReference.of("Local Raw UDP"),
                        ReferenceMapUtils.toReferenceMap(configuration),
                        Collections.emptyMap(),
                        ValueReference.of("org.graylog2.inputs.raw.udp.RawUDPInput"),
                        ValueReference.of(false),
                        Collections.emptyList()), JsonNode.class))
                .build();
        final Optional<NativeEntity<InputWithExtractors>> existingInput = facade.findExisting(entity, Collections.emptyMap());
        assertThat(existingInput).isEmpty();
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/inputs.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void resolveEntityDescriptor() {
        final EntityDescriptor descriptor = EntityDescriptor.create("5acc84f84b900a4ff290d9a7", ModelTypes.INPUT_V1);
        final Graph<EntityDescriptor> graph = facade.resolveNativeEntity(descriptor);
        assertThat(graph.nodes()).containsOnly(descriptor);
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/inputs.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void resolveEntity() {
        final Map<String, Object> configuration = new HashMap<>();
        configuration.put("override_source", null);
        configuration.put("recv_buffer_size", 262144);
        configuration.put("bind_address", "127.0.0.1");
        configuration.put("port", 5555);
        configuration.put("number_worker_threads", 8);
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("5acc84f84b900a4ff290d9a7"))
                .type(ModelTypes.INPUT_V1)
                .data(objectMapper.convertValue(InputEntity.create(
                        ValueReference.of("Local Raw UDP"),
                        ReferenceMapUtils.toReferenceMap(configuration),
                        Collections.emptyMap(),
                        ValueReference.of("org.graylog2.inputs.raw.udp.RawUDPInput"),
                        ValueReference.of(false),
                        Collections.emptyList()), JsonNode.class))
                .build();
        final Graph<Entity> graph = facade.resolveForInstallation(entity, Collections.emptyMap(), Collections.emptyMap());
        assertThat(graph.nodes()).containsOnly(entity);
    }


    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/inputs.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void delete() throws NotFoundException {
        final Input input = inputService.find("5acc84f84b900a4ff290d9a7");
        final InputWithExtractors inputWithExtractors = InputWithExtractors.create(input);

        assertThat(inputService.totalCount()).isEqualTo(2L);
        facade.delete(inputWithExtractors);

        assertThat(inputService.totalCount()).isEqualTo(1L);
        assertThatThrownBy(() -> inputService.find("5acc84f84b900a4ff290d9a7"))
                .isInstanceOf(NotFoundException.class);
    }

}
