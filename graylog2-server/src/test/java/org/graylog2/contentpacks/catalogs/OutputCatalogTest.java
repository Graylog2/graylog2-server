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
package org.graylog2.contentpacks.catalogs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.contentpacks.codecs.OutputCodec;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.OutputEntity;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.outputs.OutputRegistry;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
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

import java.util.Optional;
import java.util.Set;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.core.api.Assertions.assertThat;

public class OutputCatalogTest {
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
    private OutputCatalog catalog;

    @Before
    public void setUp() throws Exception {
        final OutputService outputService = new OutputServiceImpl(mongoRule.getMongoConnection(), new MongoJackObjectMapperProvider(objectMapper), streamService, outputRegistry);
        final OutputCodec codec = new OutputCodec(objectMapper, outputService);

        catalog = new OutputCatalog(outputService, codec);
    }

    @Test
    public void supports() {
        assertThat(catalog.supports(ModelType.of("foobar"))).isFalse();
        assertThat(catalog.supports(ModelType.of("output"))).isTrue();
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/outputs.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void listEntityExcerpts() {
        final EntityExcerpt expectedEntityExcerpt = EntityExcerpt.builder()
                .id(ModelId.of("5adf239e4b900a0fdb4e5197"))
                .type(ModelTypes.OUTPUT)
                .title("STDOUT")
                .build();

        final Set<EntityExcerpt> entityExcerpts = catalog.listEntityExcerpts();
        assertThat(entityExcerpts).containsOnly(expectedEntityExcerpt);
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/outputs.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void collectEntity() {
        final Optional<Entity> collectedEntity = catalog.collectEntity(EntityDescriptor.create(ModelId.of("5adf239e4b900a0fdb4e5197"), ModelTypes.OUTPUT));
        assertThat(collectedEntity)
                .isPresent()
                .containsInstanceOf(EntityV1.class);

        final EntityV1 entity = (EntityV1) collectedEntity.orElseThrow(AssertionError::new);
        assertThat(entity.id()).isEqualTo(ModelId.of("5adf239e4b900a0fdb4e5197"));
        assertThat(entity.type()).isEqualTo(ModelTypes.OUTPUT);
        final OutputEntity outputEntity = objectMapper.convertValue(entity.data(), OutputEntity.class);
        assertThat(outputEntity.title()).isEqualTo("STDOUT");
        assertThat(outputEntity.type()).isEqualTo("org.graylog2.outputs.LoggingOutput");
        assertThat(outputEntity.configuration()).isNotEmpty();
    }
}
