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
import org.graylog2.alarmcallbacks.AlarmCallbackConfigurationService;
import org.graylog2.alerts.AlertService;
import org.graylog2.contentpacks.codecs.StreamCodec;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.EntityWithConstraints;
import org.graylog2.contentpacks.model.entities.StreamEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.indexer.MongoIndexSet;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.streams.OutputImpl;
import org.graylog2.streams.OutputService;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamRuleServiceImpl;
import org.graylog2.streams.StreamService;
import org.graylog2.streams.StreamServiceImpl;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class StreamCatalogTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();

    @Rule
    public final MongoConnectionRule mongoRule = MongoConnectionRule.build("test");

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Mock
    private AlertService alertService;
    @Mock
    private OutputService outputService;
    @Mock
    private IndexSetService indexSetService;
    @Mock
    private MongoIndexSet.Factory mongoIndexSetFactory;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AlarmCallbackConfigurationService alarmCallbackConfigurationService;
    private StreamCatalog catalog;

    @Before
    public void setUp() throws Exception {
        final MongoConnection mongoConnection = mongoRule.getMongoConnection();
        final StreamRuleService streamRuleService = new StreamRuleServiceImpl(mongoConnection);
        final ClusterEventBus clusterEventBus = new ClusterEventBus();
        final StreamService streamService = new StreamServiceImpl(
                mongoConnection,
                streamRuleService,
                alertService,
                outputService,
                indexSetService,
                mongoIndexSetFactory,
                notificationService,
                clusterEventBus,
                alarmCallbackConfigurationService);
        final StreamCodec codec = new StreamCodec(objectMapper, streamService, streamRuleService, indexSetService);
        when(outputService.load("5adf239e4b900a0fdb4e5197")).thenReturn(
                OutputImpl.create("5adf239e4b900a0fdb4e5197", "Title", "Type", "admin", Collections.emptyMap(), new Date(1524654085L), null)
        );

        catalog = new StreamCatalog(streamService, codec);
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/streams.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void listEntityExcerpts() {
        final EntityExcerpt expectedEntityExcerpt1 = EntityExcerpt.builder()
                .id(ModelId.of("000000000000000000000001"))
                .type(ModelTypes.STREAM)
                .title("All messages")
                .build();
        final EntityExcerpt expectedEntityExcerpt2 = EntityExcerpt.builder()
                .id(ModelId.of("5adf23894b900a0fdb4e517d"))
                .type(ModelTypes.STREAM)
                .title("Test")
                .build();

        final Set<EntityExcerpt> entityExcerpts = catalog.listEntityExcerpts();
        assertThat(entityExcerpts).containsOnly(expectedEntityExcerpt1, expectedEntityExcerpt2);
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/streams.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void collectEntity() {
        final Optional<EntityWithConstraints> collectedEntity = catalog.collectEntity(EntityDescriptor.create(ModelId.of("5adf23894b900a0fdb4e517d"), ModelTypes.STREAM));
        assertThat(collectedEntity)
                .isPresent()
                .map(EntityWithConstraints::entity)
                .containsInstanceOf(EntityV1.class);

        final EntityV1 entity = (EntityV1) collectedEntity.map(EntityWithConstraints::entity).orElseThrow(AssertionError::new);
        assertThat(entity.id()).isEqualTo(ModelId.of("5adf23894b900a0fdb4e517d"));
        assertThat(entity.type()).isEqualTo(ModelTypes.STREAM);
        final StreamEntity streamEntity = objectMapper.convertValue(entity.data(), StreamEntity.class);
        assertThat(streamEntity.title()).isEqualTo(ValueReference.of("Test"));
        assertThat(streamEntity.description()).isEqualTo(ValueReference.of("Description"));
        assertThat(streamEntity.matchingType()).isEqualTo(ValueReference.of(Stream.MatchingType.AND));
        assertThat(streamEntity.streamRules()).hasSize(7);
    }
}
