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
import org.graylog2.contentpacks.model.entities.LookupTableEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.lookup.LookupDefaultValue;
import org.graylog2.lookup.db.DBLookupTableService;
import org.graylog2.lookup.dto.LookupTableDto;
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

public class LookupTableFacadeTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();

    @Rule
    public final MongoConnectionRule mongoRule = MongoConnectionRule.build("test");

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    private LookupTableFacade facade;

    @Before
    @SuppressForbidden("Using Executors.newSingleThreadExecutor() is okay in tests")
    public void setUp() throws Exception {
        final ClusterEventBus clusterEventBus = new ClusterEventBus("cluster-event-bus", Executors.newSingleThreadExecutor());
        final DBLookupTableService lookupTableService = new DBLookupTableService(
                mongoRule.getMongoConnection(),
                new MongoJackObjectMapperProvider(objectMapper),
                clusterEventBus);

        facade = new LookupTableFacade(objectMapper, lookupTableService);
    }

    @Test
    public void encode() {
        final LookupTableDto lookupTableDto = LookupTableDto.builder()
                .id("1234567890")
                .name("lookup-table-name")
                .title("Lookup Table Title")
                .description("Lookup Table Description")
                .dataAdapterId("data-adapter-1234")
                .cacheId("cache-1234")
                .defaultSingleValue("default-single")
                .defaultSingleValueType(LookupDefaultValue.Type.STRING)
                .defaultMultiValue("default-multi")
                .defaultMultiValueType(LookupDefaultValue.Type.STRING)
                .build();
        final EntityWithConstraints entityWithConstraints = facade.encode(lookupTableDto);
        final Entity entity = entityWithConstraints.entity();

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of("1234567890"));
        assertThat(entity.type()).isEqualTo(ModelType.of("lookup_table"));

        final EntityV1 entityV1 = (EntityV1) entity;
        final LookupTableEntity lookupTableEntity = objectMapper.convertValue(entityV1.data(), LookupTableEntity.class);
        assertThat(lookupTableEntity.name()).isEqualTo(ValueReference.of("lookup-table-name"));
        assertThat(lookupTableEntity.title()).isEqualTo(ValueReference.of("Lookup Table Title"));
        assertThat(lookupTableEntity.description()).isEqualTo(ValueReference.of("Lookup Table Description"));
        assertThat(lookupTableEntity.dataAdapterName()).isEqualTo(ValueReference.of("data-adapter-1234"));
        assertThat(lookupTableEntity.cacheName()).isEqualTo(ValueReference.of("cache-1234"));
        assertThat(lookupTableEntity.defaultSingleValue()).isEqualTo(ValueReference.of("default-single"));
        assertThat(lookupTableEntity.defaultSingleValueType()).isEqualTo(ValueReference.of(LookupDefaultValue.Type.STRING));
        assertThat(lookupTableEntity.defaultMultiValue()).isEqualTo(ValueReference.of("default-multi"));
        assertThat(lookupTableEntity.defaultMultiValueType()).isEqualTo(ValueReference.of(LookupDefaultValue.Type.STRING));
    }

    @Test
    public void createExcerpt() {
        final LookupTableDto lookupTableDto = LookupTableDto.builder()
                .id("1234567890")
                .name("lookup-table-name")
                .title("Lookup Table Title")
                .description("Lookup Table Description")
                .dataAdapterId("data-adapter-1234")
                .cacheId("cache-1234")
                .defaultSingleValue("default-single")
                .defaultSingleValueType(LookupDefaultValue.Type.STRING)
                .defaultMultiValue("default-multi")
                .defaultMultiValueType(LookupDefaultValue.Type.STRING)
                .build();
        final EntityExcerpt excerpt = facade.createExcerpt(lookupTableDto);

        assertThat(excerpt.id()).isEqualTo(ModelId.of("lookup-table-name"));
        assertThat(excerpt.type()).isEqualTo(ModelType.of("lookup_table"));
        assertThat(excerpt.title()).isEqualTo("Lookup Table Title");
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/lut_tables.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void listEntityExcerpts() {
        final EntityExcerpt expectedEntityExcerpt = EntityExcerpt.builder()
                .id(ModelId.of("http-dsv-no-cache"))
                .type(ModelTypes.LOOKUP_TABLE)
                .title("HTTP DSV without Cache")
                .build();

        final Set<EntityExcerpt> entityExcerpts = facade.listEntityExcerpts();
        assertThat(entityExcerpts).containsOnly(expectedEntityExcerpt);
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/lut_tables.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void collectEntity() {
        final Optional<EntityWithConstraints> collectedEntity = facade.collectEntity(EntityDescriptor.create(ModelId.of("http-dsv-no-cache"), ModelTypes.LOOKUP_TABLE));
        assertThat(collectedEntity)
                .isPresent()
                .map(EntityWithConstraints::entity)
                .containsInstanceOf(EntityV1.class);

        final EntityV1 entity = (EntityV1) collectedEntity.map(EntityWithConstraints::entity).orElseThrow(AssertionError::new);
        assertThat(entity.id()).isEqualTo(ModelId.of("5adf24dd4b900a0fdb4e530d"));
        assertThat(entity.type()).isEqualTo(ModelTypes.LOOKUP_TABLE);
        final LookupTableEntity lookupTableEntity = objectMapper.convertValue(entity.data(), LookupTableEntity.class);
        assertThat(lookupTableEntity.name()).isEqualTo(ValueReference.of("http-dsv-no-cache"));
        assertThat(lookupTableEntity.title()).isEqualTo(ValueReference.of("HTTP DSV without Cache"));
        assertThat(lookupTableEntity.description()).isEqualTo(ValueReference.of("HTTP DSV without Cache"));
    }
}
