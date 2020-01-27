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
package org.graylog.events.contentpack.facade;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import org.graylog.events.contentpack.entities.EmailEventNotificationConfigEntity;
import org.graylog.events.contentpack.entities.HttpEventNotificationConfigEntity;
import org.graylog.events.contentpack.entities.NotificationEntity;
import org.graylog.events.notifications.DBNotificationService;
import org.graylog.events.notifications.NotificationDto;
import org.graylog.events.notifications.NotificationResourceHandler;
import org.graylog.events.notifications.types.EmailEventNotificationConfig;
import org.graylog.events.notifications.types.HTTPEventNotificationConfig;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.events.processor.DBEventProcessorStateService;
import org.graylog.scheduler.DBJobDefinitionService;
import org.graylog.scheduler.JobDefinitionDto;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.*;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NotificationFacadeTest {

    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();

    private ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Rule
    public MongoConnectionRule mongoRule = MongoConnectionRule.build("test");

    private NotificationFacade facade;

    @Mock
    private DBJobDefinitionService jobDefinitionService;

    @Mock
    private DBEventProcessorStateService stateService;

    @Mock
    private DBEventDefinitionService eventDefinitionService;

    @Mock
    private DBNotificationService notificationService;

    @Mock
    private NotificationResourceHandler notificationResourceHandler;

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private MongoJackObjectMapperProvider mapperProvider = new MongoJackObjectMapperProvider(objectMapper);

    @Before
    @SuppressForbidden("Using Executors.newSingleThreadExecutor() is okay in tests")
    public void setUp() throws Exception {
        objectMapper.registerSubtypes(
                EmailEventNotificationConfig.class,
                EmailEventNotificationConfigEntity.class,
                HttpEventNotificationConfigEntity.class,
                HTTPEventNotificationConfig.class
        );
        jobDefinitionService = mock(DBJobDefinitionService.class);
        stateService = mock(DBEventProcessorStateService.class);
        eventDefinitionService = new DBEventDefinitionService(mongoRule.getMongoConnection(), mapperProvider, stateService);

        notificationService = new DBNotificationService(mongoRule.getMongoConnection(), mapperProvider);
        notificationResourceHandler = new NotificationResourceHandler(notificationService, jobDefinitionService, eventDefinitionService, Maps.newHashMap());
        facade = new NotificationFacade(objectMapper, notificationResourceHandler, notificationService);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void exportEntity() {
        final ModelId id = ModelId.of("5d4d33753d27460ad18e0c4d");
        final EntityDescriptor descriptor = EntityDescriptor.create(id, ModelTypes.NOTIFICATION_V1);
        final EntityDescriptorIds entityDescriptorIds = EntityDescriptorIds.of(descriptor);
        final Optional<Entity> entity = facade.exportEntity(descriptor, entityDescriptorIds);
        assertThat(entity).isPresent();
        final EntityV1 entityV1 = (EntityV1) entity.get();
        final NotificationEntity notificationEntity = objectMapper.convertValue(entityV1.data(),
                NotificationEntity.class);
        assertThat(notificationEntity.title().asString()).isEqualTo("title");
        assertThat(notificationEntity.description().asString()).isEqualTo("description");
        assertThat(notificationEntity.config().type()).isEqualTo("email-notification-v1");
    }

    private EntityV1 createTestEntity() {
        final HttpEventNotificationConfigEntity httpEventNotificationConfigEntity =
                HttpEventNotificationConfigEntity.builder()
                        .url(ValueReference.of("https://hulud.net"))
                        .build();
        final NotificationEntity notificationEntity = NotificationEntity.builder()
                .title(ValueReference.of("title"))
                .description(ValueReference.of("descriptions"))
                .config(httpEventNotificationConfigEntity)
                .build();

        final JsonNode data = objectMapper.convertValue(notificationEntity, JsonNode.class);
        return EntityV1.builder()
                .data(data)
                .id(ModelId.of("beef-1337"))
                .type(ModelTypes.NOTIFICATION_V1)
                .build();
    }

    @Test
    public void createNativeEntity() {
        final EntityV1 entityV1 = createTestEntity();
        final JobDefinitionDto jobDefinitionDto = mock(JobDefinitionDto.class);

        when(jobDefinitionService.save(any(JobDefinitionDto.class))).thenReturn(jobDefinitionDto);

        final NativeEntity<NotificationDto> nativeEntity = facade.createNativeEntity(
            entityV1,
            ImmutableMap.of(),
            ImmutableMap.of(),
            "kmerz");
        assertThat(nativeEntity).isNotNull();

        final NotificationDto notificationDto = nativeEntity.entity();
        assertThat(notificationDto.title()).isEqualTo("title");
        assertThat(notificationDto.description()).isEqualTo("descriptions");
        assertThat(notificationDto.config().type()).isEqualTo("http-notification-v1");
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void loadNativeEntity() {
        final NativeEntityDescriptor nativeEntityDescriptor = NativeEntityDescriptor.create(
                ModelId.of("content-pack-id"),
                ModelId.of("5d4d33753d27460ad18e0c4d"),
                ModelTypes.NOTIFICATION_V1,
                "title");
        final Optional<NativeEntity<NotificationDto>> optionalNativeEntity = facade.loadNativeEntity(
                nativeEntityDescriptor);
        assertThat(optionalNativeEntity).isPresent();
        final NativeEntity<NotificationDto> nativeEntity = optionalNativeEntity.get();
        assertThat(nativeEntity.entity()).isNotNull();
        final NotificationDto notificationDto = nativeEntity.entity();
        assertThat(notificationDto.id()).isEqualTo("5d4d33753d27460ad18e0c4d");
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void createExcerpt() {
        final Optional<NotificationDto> notificationDto = notificationService.get(
                "5d4d33753d27460ad18e0c4d");
        assertThat(notificationDto).isPresent();
        final EntityExcerpt excerpt = facade.createExcerpt(notificationDto.get());
        assertThat(excerpt.title()).isEqualTo("title");
        assertThat(excerpt.id()).isEqualTo(ModelId.of("5d4d33753d27460ad18e0c4d"));
        assertThat(excerpt.type()).isEqualTo(ModelTypes.NOTIFICATION_V1);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void listExcerpts() {
        final Set<EntityExcerpt> excerpts = facade.listEntityExcerpts();
        final EntityExcerpt excerpt = excerpts.iterator().next();
        assertThat(excerpt.title()).isEqualTo("title");
        assertThat(excerpt.id()).isEqualTo(ModelId.of("5d4d33753d27460ad18e0c4d"));
        assertThat(excerpt.type()).isEqualTo(ModelTypes.NOTIFICATION_V1);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void delete() {
        long countBefore = notificationService.streamAll().count();
        assertThat(countBefore).isEqualTo(1);

        final Optional<NotificationDto> notificationDto = notificationService.get(
                "5d4d33753d27460ad18e0c4d");
        assertThat(notificationDto).isPresent();
        facade.delete(notificationDto.get());

        long countAfter = notificationService.streamAll().count();
        assertThat(countAfter).isEqualTo(0);
    }
}
