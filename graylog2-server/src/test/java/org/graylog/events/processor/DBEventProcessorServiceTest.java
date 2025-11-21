/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.events.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.google.common.collect.ImmutableList;
import org.graylog.events.TestEventProcessorConfig;
import org.graylog.events.notifications.EventNotificationSettings;
import org.graylog.events.processor.storage.PersistToStreamsStorageHandler;
import org.graylog.plugins.views.search.searchfilters.db.IgnoreSearchFilters;
import org.graylog.security.entities.EntityRegistrar;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.entities.DefaultEntityScope;
import org.graylog2.database.entities.EntityScope;
import org.graylog2.database.entities.EntityScopeService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MongoDBExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class DBEventProcessorServiceTest {
    public static final Set<EntityScope> ENTITY_SCOPES = Collections.singleton(new DefaultEntityScope());
    private static final String REMEDIATION_STEPS = "Remediation steps";

    @Mock
    private DBEventProcessorStateService stateService;

    private DBEventDefinitionService dbService;

    @BeforeEach
    public void setUp(MongoDBTestService dbTestService) throws Exception {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        objectMapper.registerSubtypes(new NamedType(TestEventProcessorConfig.class, TestEventProcessorConfig.TYPE_NAME));
        objectMapper.registerSubtypes(new NamedType(PersistToStreamsStorageHandler.Config.class, PersistToStreamsStorageHandler.Config.TYPE_NAME));
        final MongoJackObjectMapperProvider mapperProvider = new MongoJackObjectMapperProvider(objectMapper);

        this.dbService = new DBEventDefinitionService(new MongoCollections(mapperProvider, dbTestService.mongoConnection()),
                stateService, mock(EntityRegistrar.class), new EntityScopeService(ENTITY_SCOPES), new IgnoreSearchFilters());
    }

    @Test
    @MongoDBFixtures("event-processors.json")
    public void loadPersisted() {
        final List<EventDefinitionDto> dtos;
        try (var stream = dbService.streamAll()) {
            dtos = stream.collect(Collectors.toList());
        }

        assertThat(dtos).hasSize(1);

        assertThat(dtos.get(0)).satisfies(dto -> {
            assertThat(dto.id()).isNotBlank();
            assertThat(dto.title()).isEqualTo("Test");
            assertThat(dto.description()).isEqualTo("A test event definition");
            assertThat(dto.priority()).isEqualTo(2);
            assertThat(dto.keySpec()).isEqualTo(ImmutableList.of("username"));
            assertThat(dto.fieldSpec()).isEmpty();
            assertThat(dto.notifications()).isEmpty();
            assertThat(dto.storage()).hasSize(1);

            assertThat(dto.config()).isInstanceOf(TestEventProcessorConfig.class);
            assertThat(dto.config()).satisfies(abstractConfig -> {
                final TestEventProcessorConfig config = (TestEventProcessorConfig) abstractConfig;

                assertThat(config.type()).isEqualTo("__test_event_processor_config__");
                assertThat(config.message()).isEqualTo("This is a test event processor");
            });
        });
    }

    @Test
    public void save() {
        final EventDefinitionDto newDto = EventDefinitionDto.builder()
                .title("Test")
                .description("A test event definition")
                .remediationSteps(REMEDIATION_STEPS)
                .config(TestEventProcessorConfig.builder()
                        .message("This is a test event processor")
                        .searchWithinMs(1000)
                        .executeEveryMs(1000)
                        .build())
                .priority(3)
                .alert(false)
                .notificationSettings(EventNotificationSettings.withGracePeriod(60000))
                .keySpec(ImmutableList.of("a", "b"))
                .notifications(ImmutableList.of())
                .build();

        final EventDefinitionDto dto = dbService.save(newDto);

        assertThat(dto.id()).isNotBlank();
        assertThat(dto.title()).isEqualTo("Test");
        assertThat(dto.description()).isEqualTo("A test event definition");
        assertThat(dto.remediationSteps()).isEqualTo(REMEDIATION_STEPS);
        assertThat(dto.priority()).isEqualTo(3);
        assertThat(dto.keySpec()).isEqualTo(ImmutableList.of("a", "b"));
        assertThat(dto.fieldSpec()).isEmpty();
        assertThat(dto.notifications()).isEmpty();
        assertThat(dto.storage()).hasSize(1);
        // We will always add a persist-to-streams handler for now
        assertThat(dto.storage()).containsOnly(PersistToStreamsStorageHandler.Config.createWithDefaultEventsStream());
    }

    @Test
    @MongoDBFixtures("user-illuminate-event-definitions.json")
    public void testCountBySource() {
        final Map<String, Long> counts = dbService.countBySource();

        assertThat(counts).isEqualTo(Map.of(
                "illuminate_event_definitions", 1L,
                "user_event_definitions", 1L
        ));
    }
}
