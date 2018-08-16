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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.graph.Graph;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.EntityWithConstraints;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.OutputEntity;
import org.graylog2.contentpacks.model.entities.references.ReferenceMapUtils;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.database.NotFoundException;
import org.graylog2.outputs.LoggingOutput;
import org.graylog2.outputs.OutputRegistry;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.streams.Output;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.streams.OutputImpl;
import org.graylog2.streams.OutputService;
import org.graylog2.streams.OutputServiceImpl;
import org.graylog2.streams.StreamService;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OutputFacadeTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();

    @Rule
    public final MongoConnectionRule mongoRule = MongoConnectionRule.build("test");

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Mock
    private StreamService streamService;
    @Mock
    private OutputRegistry outputRegistry;
    private Set<PluginMetaData> pluginMetaData;
    private OutputService outputService;
    private OutputFacade facade;
    private Map<String, MessageOutput.Factory<? extends MessageOutput>> outputFactories;

    @Before
    public void setUp() throws Exception {
        outputService = new OutputServiceImpl(mongoRule.getMongoConnection(), new MongoJackObjectMapperProvider(objectMapper), streamService, outputRegistry);
        pluginMetaData = new HashSet<>();
        outputFactories = new HashMap<>();
        final LoggingOutput.Factory factory = mock(LoggingOutput.Factory.class);
        final LoggingOutput.Descriptor descriptor = mock(LoggingOutput.Descriptor.class);
        when(factory.getDescriptor()).thenReturn(descriptor);
        outputFactories.put("org.graylog2.outputs.LoggingOutput", factory);

        facade = new OutputFacade(objectMapper, outputService, pluginMetaData, outputFactories);
    }

    @Test
    public void exportEntity() {
        final ImmutableMap<String, Object> configuration = ImmutableMap.of(
                "some-setting", "foobar"
        );
        final OutputImpl output = OutputImpl.create(
                "01234567890",
                "Output Title",
                "org.graylog2.outputs.LoggingOutput",
                "admin",
                configuration,
                new Date(0L),
                null
        );
        final EntityWithConstraints entityWithConstraints = facade.exportNativeEntity(output);
        final Entity entity = entityWithConstraints.entity();

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of("01234567890"));
        assertThat(entity.type()).isEqualTo(ModelTypes.OUTPUT_V1);

        final EntityV1 entityV1 = (EntityV1) entity;
        final OutputEntity outputEntity = objectMapper.convertValue(entityV1.data(), OutputEntity.class);
        assertThat(outputEntity.title()).isEqualTo(ValueReference.of("Output Title"));
        assertThat(outputEntity.type()).isEqualTo(ValueReference.of("org.graylog2.outputs.LoggingOutput"));
        assertThat(outputEntity.configuration()).containsEntry("some-setting", ValueReference.of("foobar"));
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/outputs.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void exportNativeEntity() throws NotFoundException {
        final Output output = outputService.load("5adf239e4b900a0fdb4e5197");

        final EntityWithConstraints entityWithConstraints = facade.exportNativeEntity(output);
        final Entity entity = entityWithConstraints.entity();

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of("5adf239e4b900a0fdb4e5197"));
        assertThat(entity.type()).isEqualTo(ModelTypes.OUTPUT_V1);

        final EntityV1 entityV1 = (EntityV1) entity;
        final OutputEntity outputEntity = objectMapper.convertValue(entityV1.data(), OutputEntity.class);
        assertThat(outputEntity.title()).isEqualTo(ValueReference.of("STDOUT"));
        assertThat(outputEntity.type()).isEqualTo(ValueReference.of("org.graylog2.outputs.LoggingOutput"));
        assertThat(outputEntity.configuration()).containsEntry("prefix", ValueReference.of("Writing message: "));
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void createNativeEntity() {
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("1"))
                .type(ModelTypes.OUTPUT_V1)
                .data(objectMapper.convertValue(OutputEntity.create(
                        ValueReference.of("STDOUT"),
                        ValueReference.of("org.graylog2.outputs.LoggingOutput"),
                        ReferenceMapUtils.toReferenceMap(ImmutableMap.of("prefix", "Writing message: "))
                ), JsonNode.class))
                .build();

        final NativeEntity<Output> nativeEntity = facade.createNativeEntity(entity, Collections.emptyMap(), Collections.emptyMap(), "username");

        assertThat(nativeEntity.descriptor().type()).isEqualTo(ModelTypes.OUTPUT_V1);
        assertThat(nativeEntity.entity().getTitle()).isEqualTo("STDOUT");
        assertThat(nativeEntity.entity().getType()).isEqualTo("org.graylog2.outputs.LoggingOutput");
        assertThat(nativeEntity.entity().getCreatorUserId()).isEqualTo("username");
        assertThat(nativeEntity.entity().getConfiguration()).containsEntry("prefix", "Writing message: ");
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/outputs.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void findExisting() {
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("1"))
                .type(ModelTypes.OUTPUT_V1)
                .data(objectMapper.convertValue(OutputEntity.create(
                        ValueReference.of("STDOUT"),
                        ValueReference.of("org.graylog2.outputs.LoggingOutput"),
                        ReferenceMapUtils.toReferenceMap(ImmutableMap.of("prefix", "Writing message: "))
                ), JsonNode.class))
                .build();
        final Optional<NativeEntity<Output>> existingOutput = facade.findExisting(entity, Collections.emptyMap());
        assertThat(existingOutput).isEmpty();
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/outputs.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void resolveEntity() {
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("5adf239e4b900a0fdb4e5197"))
                .type(ModelTypes.OUTPUT_V1)
                .data(objectMapper.convertValue(OutputEntity.create(
                        ValueReference.of("STDOUT"),
                        ValueReference.of("org.graylog2.outputs.LoggingOutput"),
                        ReferenceMapUtils.toReferenceMap(ImmutableMap.of("prefix", "Writing message: "))
                ), JsonNode.class))
                .build();
        final Graph<Entity> graph = facade.resolveForInstallation(entity, Collections.emptyMap(), Collections.emptyMap());
        assertThat(graph.nodes()).containsOnly(entity);
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/outputs.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void resolveEntityDescriptor() {
        final EntityDescriptor descriptor = EntityDescriptor.create("5adf239e4b900a0fdb4e5197", ModelTypes.OUTPUT_V1);
        final Graph<EntityDescriptor> graph = facade.resolveNativeEntity(descriptor);
        assertThat(graph.nodes()).containsOnly(descriptor);
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/outputs.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void delete() throws NotFoundException {
        final Output output = outputService.load("5adf239e4b900a0fdb4e5197");
        assertThat(outputService.count()).isEqualTo(1L);
        facade.delete(output);
        assertThat(outputService.count()).isEqualTo(0L);
        assertThatThrownBy(() -> outputService.load("5adf239e4b900a0fdb4e5197"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    public void createExcerpt() {
        final ImmutableMap<String, Object> configuration = ImmutableMap.of();
        final OutputImpl output = OutputImpl.create(
                "01234567890",
                "Output Title",
                "org.graylog2.output.SomeOutputClass",
                "admin",
                configuration,
                new Date(0L),
                null
        );
        final EntityExcerpt excerpt = facade.createExcerpt(output);

        assertThat(excerpt.id()).isEqualTo(ModelId.of(output.getId()));
        assertThat(excerpt.type()).isEqualTo(ModelTypes.OUTPUT_V1);
        assertThat(excerpt.title()).isEqualTo(output.getTitle());
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/outputs.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void listEntityExcerpts() {
        final EntityExcerpt expectedEntityExcerpt = EntityExcerpt.builder()
                .id(ModelId.of("5adf239e4b900a0fdb4e5197"))
                .type(ModelTypes.OUTPUT_V1)
                .title("STDOUT")
                .build();

        final Set<EntityExcerpt> entityExcerpts = facade.listEntityExcerpts();
        assertThat(entityExcerpts).containsOnly(expectedEntityExcerpt);
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/outputs.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void collectEntity() {
        final Optional<EntityWithConstraints> collectedEntity = facade.exportEntity(EntityDescriptor.create("5adf239e4b900a0fdb4e5197", ModelTypes.OUTPUT_V1));
        assertThat(collectedEntity)
                .isPresent()
                .map(EntityWithConstraints::entity)
                .containsInstanceOf(EntityV1.class);

        final EntityV1 entity = (EntityV1) collectedEntity.map(EntityWithConstraints::entity).orElseThrow(AssertionError::new);
        assertThat(entity.id()).isEqualTo(ModelId.of("5adf239e4b900a0fdb4e5197"));
        assertThat(entity.type()).isEqualTo(ModelTypes.OUTPUT_V1);
        final OutputEntity outputEntity = objectMapper.convertValue(entity.data(), OutputEntity.class);
        assertThat(outputEntity.title()).isEqualTo(ValueReference.of("STDOUT"));
        assertThat(outputEntity.type()).isEqualTo(ValueReference.of("org.graylog2.outputs.LoggingOutput"));
        assertThat(outputEntity.configuration()).isNotEmpty();
    }
}
