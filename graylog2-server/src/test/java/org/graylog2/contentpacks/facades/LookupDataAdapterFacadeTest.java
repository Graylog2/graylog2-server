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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.EntityWithConstraints;
import org.graylog2.contentpacks.model.entities.LookupDataAdapterEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.lookup.db.DBDataAdapterService;
import org.graylog2.lookup.dto.DataAdapterDto;
import org.graylog2.plugin.lookup.FallbackAdapterConfig;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

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

    @Before
    @SuppressForbidden("Using Executors.newSingleThreadExecutor() is okay in tests")
    public void setUp() throws Exception {
        final ClusterEventBus clusterEventBus = new ClusterEventBus("cluster-event-bus", Executors.newSingleThreadExecutor());
        final DBDataAdapterService dataAdapterService = new DBDataAdapterService(
                mongoRule.getMongoConnection(),
                new MongoJackObjectMapperProvider(objectMapper),
                clusterEventBus);

        facade = new LookupDataAdapterFacade(objectMapper, dataAdapterService);
    }

    @Test
    public void encode() {
        final DataAdapterDto dataAdapterDto = DataAdapterDto.builder()
                .id("1234567890")
                .name("data-adapter-name")
                .title("Data Adapter Title")
                .description("Data Adapter Description")
                .config(new FallbackAdapterConfig())
                .build();
        final EntityWithConstraints entityWithConstraints = facade.encode(dataAdapterDto);
        final Entity entity = entityWithConstraints.entity();

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of(dataAdapterDto.id()));
        assertThat(entity.type()).isEqualTo(ModelType.of("lookup_adapter"));

        final EntityV1 entityV1 = (EntityV1) entity;
        final LookupDataAdapterEntity lookupDataAdapterEntity = objectMapper.convertValue(entityV1.data(), LookupDataAdapterEntity.class);
        assertThat(lookupDataAdapterEntity.name()).isEqualTo(ValueReference.of("data-adapter-name"));
        assertThat(lookupDataAdapterEntity.title()).isEqualTo(ValueReference.of("Data Adapter Title"));
        assertThat(lookupDataAdapterEntity.description()).isEqualTo(ValueReference.of("Data Adapter Description"));
        assertThat(lookupDataAdapterEntity.configuration()).containsEntry("type", ValueReference.of("FallbackAdapterConfig"));
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
        assertThat(excerpt.type()).isEqualTo(ModelType.of("lookup_adapter"));
        assertThat(excerpt.title()).isEqualTo("Data Adapter Title");
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/lut_data_adapters.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void listEntityExcerpts() {
        final EntityExcerpt expectedEntityExcerpt = EntityExcerpt.builder()
                .id(ModelId.of("http-dsv"))
                .type(ModelTypes.LOOKUP_ADAPTER)
                .title("HTTP DSV")
                .build();

        final Set<EntityExcerpt> entityExcerpts = facade.listEntityExcerpts();
        assertThat(entityExcerpts).containsOnly(expectedEntityExcerpt);
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/lut_data_adapters.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void collectEntity() {
        final Optional<EntityWithConstraints> collectedEntity = facade.collectEntity(EntityDescriptor.create(ModelId.of("http-dsv"), ModelTypes.LOOKUP_ADAPTER));
        assertThat(collectedEntity)
                .isPresent()
                .map(EntityWithConstraints::entity)
                .containsInstanceOf(EntityV1.class);

        final EntityV1 entity = (EntityV1) collectedEntity.map(EntityWithConstraints::entity).orElseThrow(AssertionError::new);
        assertThat(entity.id()).isEqualTo(ModelId.of("5adf24a04b900a0fdb4e52c8"));
        assertThat(entity.type()).isEqualTo(ModelTypes.LOOKUP_ADAPTER);
        final LookupDataAdapterEntity lookupDataAdapterEntity = objectMapper.convertValue(entity.data(), LookupDataAdapterEntity.class);
        assertThat(lookupDataAdapterEntity.name()).isEqualTo(ValueReference.of("http-dsv"));
        assertThat(lookupDataAdapterEntity.title()).isEqualTo(ValueReference.of("HTTP DSV"));
        assertThat(lookupDataAdapterEntity.description()).isEqualTo(ValueReference.of("HTTP DSV"));
    }
}
