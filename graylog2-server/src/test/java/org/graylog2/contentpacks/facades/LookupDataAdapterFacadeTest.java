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
import org.graylog2.contentpacks.model.entities.LookupDataAdapterEntity;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.references.ReferenceMapUtils;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.lookup.db.DBDataAdapterService;
import org.graylog2.lookup.dto.DataAdapterDto;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.lookup.FallbackAdapterConfig;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.core.api.Assertions.assertThat;

public class LookupDataAdapterFacadeTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();

    @Rule
    public final MongoConnectionRule mongoRule = MongoConnectionRule.build("test");

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    private LookupDataAdapterFacade facade;
    private DBDataAdapterService dataAdapterService;
    private Set<PluginMetaData> pluginMetaData;

    @Before
    @SuppressForbidden("Using Executors.newSingleThreadExecutor() is okay in tests")
    public void setUp() throws Exception {
        final ClusterEventBus clusterEventBus = new ClusterEventBus("cluster-event-bus", Executors.newSingleThreadExecutor());
        dataAdapterService = new DBDataAdapterService(
                mongoRule.getMongoConnection(),
                new MongoJackObjectMapperProvider(objectMapper),
                clusterEventBus);
        pluginMetaData = new HashSet<>();

        facade = new LookupDataAdapterFacade(objectMapper, dataAdapterService, pluginMetaData);
    }

    @Test
    public void exportNativeEntity() {
        final DataAdapterDto dataAdapterDto = DataAdapterDto.builder()
                .id("1234567890")
                .name("data-adapter-name")
                .title("Data Adapter Title")
                .description("Data Adapter Description")
                .config(new FallbackAdapterConfig())
                .build();
        final EntityWithConstraints entityWithConstraints = facade.exportNativeEntity(dataAdapterDto);
        final Entity entity = entityWithConstraints.entity();

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of("1234567890"));
        assertThat(entity.type()).isEqualTo(ModelTypes.LOOKUP_ADAPTER_V1);

        final EntityV1 entityV1 = (EntityV1) entity;
        final LookupDataAdapterEntity lookupDataAdapterEntity = objectMapper.convertValue(entityV1.data(), LookupDataAdapterEntity.class);
        assertThat(lookupDataAdapterEntity.name()).isEqualTo(ValueReference.of("data-adapter-name"));
        assertThat(lookupDataAdapterEntity.title()).isEqualTo(ValueReference.of("Data Adapter Title"));
        assertThat(lookupDataAdapterEntity.description()).isEqualTo(ValueReference.of("Data Adapter Description"));
        assertThat(lookupDataAdapterEntity.configuration()).containsEntry("type", ValueReference.of("FallbackAdapterConfig"));
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/lut_data_adapters.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void exportEntityDescriptor() {
        final EntityDescriptor descriptor = EntityDescriptor.create("5adf24a04b900a0fdb4e52c8", ModelTypes.LOOKUP_ADAPTER_V1);
        final EntityWithConstraints entityWithConstraints = facade.exportEntity(descriptor).orElseThrow(AssertionError::new);
        final Entity entity = entityWithConstraints.entity();

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of("5adf24a04b900a0fdb4e52c8"));
        assertThat(entity.type()).isEqualTo(ModelTypes.LOOKUP_ADAPTER_V1);

        final EntityV1 entityV1 = (EntityV1) entity;
        final LookupDataAdapterEntity lookupDataAdapterEntity = objectMapper.convertValue(entityV1.data(), LookupDataAdapterEntity.class);
        assertThat(lookupDataAdapterEntity.name()).isEqualTo(ValueReference.of("http-dsv"));
        assertThat(lookupDataAdapterEntity.title()).isEqualTo(ValueReference.of("HTTP DSV"));
        assertThat(lookupDataAdapterEntity.description()).isEqualTo(ValueReference.of("HTTP DSV"));
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void createNativeEntity() {
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("1"))
                .type(ModelTypes.LOOKUP_ADAPTER_V1)
                .data(objectMapper.convertValue(LookupDataAdapterEntity.create(
                        ValueReference.of("http-dsv"),
                        ValueReference.of("HTTP DSV"),
                        ValueReference.of("HTTP DSV"),
                        ReferenceMapUtils.toReferenceMap(Collections.emptyMap())
                ), JsonNode.class))
                .build();
        assertThat(dataAdapterService.findAll()).isEmpty();

        final NativeEntity<DataAdapterDto> nativeEntity = facade.createNativeEntity(entity, Collections.emptyMap(), Collections.emptyMap(), "username");

        assertThat(nativeEntity.descriptor().id()).isEqualTo(ModelId.of("http-dsv"));
        assertThat(nativeEntity.descriptor().type()).isEqualTo(ModelTypes.LOOKUP_ADAPTER_V1);
        assertThat(nativeEntity.entity().name()).isEqualTo("http-dsv");
        assertThat(nativeEntity.entity().title()).isEqualTo("HTTP DSV");
        assertThat(nativeEntity.entity().description()).isEqualTo("HTTP DSV");

        assertThat(dataAdapterService.findAll()).hasSize(1);
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/lut_data_adapters.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void findExisting() {
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("1"))
                .type(ModelTypes.LOOKUP_ADAPTER_V1)
                .data(objectMapper.convertValue(LookupDataAdapterEntity.create(
                        ValueReference.of("http-dsv"),
                        ValueReference.of("HTTP DSV"),
                        ValueReference.of("HTTP DSV"),
                        ReferenceMapUtils.toReferenceMap(Collections.emptyMap())
                ), JsonNode.class))
                .build();
        final NativeEntity<DataAdapterDto> nativeEntity = facade.findExisting(entity, Collections.emptyMap()).orElseThrow(AssertionError::new);

        assertThat(nativeEntity.descriptor().id()).isEqualTo(ModelId.of("5adf24a04b900a0fdb4e52c8"));
        assertThat(nativeEntity.descriptor().type()).isEqualTo(ModelTypes.LOOKUP_ADAPTER_V1);
        assertThat(nativeEntity.entity().name()).isEqualTo("http-dsv");
        assertThat(nativeEntity.entity().title()).isEqualTo("HTTP DSV");
        assertThat(nativeEntity.entity().description()).isEqualTo("HTTP DSV");
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/lut_data_adapters.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void findExistingWithNoExistingEntity() {
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("1"))
                .type(ModelTypes.LOOKUP_ADAPTER_V1)
                .data(objectMapper.convertValue(LookupDataAdapterEntity.create(
                        ValueReference.of("some-name"),
                        ValueReference.of("Some title"),
                        ValueReference.of("Some description"),
                        ReferenceMapUtils.toReferenceMap(Collections.emptyMap())
                ), JsonNode.class))
                .build();
        final Optional<NativeEntity<DataAdapterDto>> existingEntity = facade.findExisting(entity, Collections.emptyMap());
        assertThat(existingEntity).isEmpty();
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/lut_data_adapters.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void delete() {
        final Optional<DataAdapterDto> dataAdapterDto = dataAdapterService.get("5adf24a04b900a0fdb4e52c8");

        assertThat(dataAdapterService.findAll()).hasSize(1);
        dataAdapterDto.ifPresent(facade::delete);

        assertThat(dataAdapterService.findAll()).isEmpty();
        assertThat(dataAdapterService.get("5adf24a04b900a0fdb4e52c8")).isEmpty();
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/lut_data_adapters.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void resolveEntityDescriptor() {
        final EntityDescriptor descriptor = EntityDescriptor.create("5adf24a04b900a0fdb4e52c8", ModelTypes.LOOKUP_ADAPTER_V1);
        final Graph<EntityDescriptor> graph = facade.resolveNativeEntity(descriptor);
        assertThat(graph.nodes()).containsOnly(descriptor);
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/lut_data_adapters.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void resolveEntity() {
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("5adf24a04b900a0fdb4e52c8"))
                .type(ModelTypes.LOOKUP_ADAPTER_V1)
                .data(objectMapper.convertValue(LookupDataAdapterEntity.create(
                        ValueReference.of("http-dsv"),
                        ValueReference.of("HTTP DSV"),
                        ValueReference.of("HTTP DSV"),
                        ReferenceMapUtils.toReferenceMap(Collections.emptyMap())
                ), JsonNode.class))
                .build();
        final Graph<Entity> graph = facade.resolveForInstallation(entity, Collections.emptyMap(), Collections.emptyMap());
        assertThat(graph.nodes()).containsOnly(entity);
    }


    @Test
    public void createExcerpt() {
        final DataAdapterDto dataAdapterDto = DataAdapterDto.builder()
                .id("1234567890")
                .name("data-adapter-name")
                .title("Data Adapter Title")
                .description("Data Adapter Description")
                .config(new FallbackAdapterConfig())
                .build();
        final EntityExcerpt excerpt = facade.createExcerpt(dataAdapterDto);

        assertThat(excerpt.id()).isEqualTo(ModelId.of("data-adapter-name"));
        assertThat(excerpt.type()).isEqualTo(ModelTypes.LOOKUP_ADAPTER_V1);
        assertThat(excerpt.title()).isEqualTo("Data Adapter Title");
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/lut_data_adapters.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void listEntityExcerpts() {
        final EntityExcerpt expectedEntityExcerpt = EntityExcerpt.builder()
                .id(ModelId.of("http-dsv"))
                .type(ModelTypes.LOOKUP_ADAPTER_V1)
                .title("HTTP DSV")
                .build();

        final Set<EntityExcerpt> entityExcerpts = facade.listEntityExcerpts();
        assertThat(entityExcerpts).containsOnly(expectedEntityExcerpt);
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/lut_data_adapters.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void collectEntity() {
        final Optional<EntityWithConstraints> collectedEntity = facade.exportEntity(EntityDescriptor.create("http-dsv", ModelTypes.LOOKUP_ADAPTER_V1));
        assertThat(collectedEntity)
                .isPresent()
                .map(EntityWithConstraints::entity)
                .containsInstanceOf(EntityV1.class);

        final EntityV1 entity = (EntityV1) collectedEntity.map(EntityWithConstraints::entity).orElseThrow(AssertionError::new);
        assertThat(entity.id()).isEqualTo(ModelId.of("5adf24a04b900a0fdb4e52c8"));
        assertThat(entity.type()).isEqualTo(ModelTypes.LOOKUP_ADAPTER_V1);
        final LookupDataAdapterEntity lookupDataAdapterEntity = objectMapper.convertValue(entity.data(), LookupDataAdapterEntity.class);
        assertThat(lookupDataAdapterEntity.name()).isEqualTo(ValueReference.of("http-dsv"));
        assertThat(lookupDataAdapterEntity.title()).isEqualTo(ValueReference.of("HTTP DSV"));
        assertThat(lookupDataAdapterEntity.description()).isEqualTo(ValueReference.of("HTTP DSV"));
    }
}
