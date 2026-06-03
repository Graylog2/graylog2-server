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
import com.google.common.collect.ImmutableSet;
import org.graylog.events.TestEventProcessorConfig;
import org.graylog.events.notifications.EventNotificationSettings;
import org.graylog.events.processor.exclusion.ExclusionRule;
import org.graylog.events.processor.exclusion.Matcher;
import org.graylog.events.processor.exclusion.MatcherType;
import org.graylog.events.processor.storage.PersistToStreamsStorageHandler;
import org.graylog.plugins.views.search.searchfilters.db.IgnoreSearchFilters;
import org.graylog.security.entities.EntityRegistrar;
import org.graylog.testing.mongodb.MongoDBExtension;
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

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MongoDBExtension.class)
public class DBEventDefinitionServiceTest {
    private static final Set<EntityScope> ENTITY_SCOPES = Collections.singleton(new DefaultEntityScope());

    @Mock
    private DBEventProcessorStateService stateService;

    private DBEventDefinitionService dbService;

    @BeforeEach
    public void setUp(MongoDBTestService dbTestService) {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        objectMapper.registerSubtypes(new NamedType(TestEventProcessorConfig.class, TestEventProcessorConfig.TYPE_NAME));
        objectMapper.registerSubtypes(new NamedType(PersistToStreamsStorageHandler.Config.class, PersistToStreamsStorageHandler.Config.TYPE_NAME));
        final MongoJackObjectMapperProvider mapperProvider = new MongoJackObjectMapperProvider(objectMapper);

        this.dbService = new DBEventDefinitionService(
                new MongoCollections(mapperProvider, dbTestService.mongoConnection()),
                stateService,
                mock(EntityRegistrar.class),
                new EntityScopeService(ENTITY_SCOPES),
                new IgnoreSearchFilters());
    }

    @Test
    public void mintsIdsForExclusionsWithoutIds() {
        final ExclusionRule ruleWithoutId = ExclusionRule.builder()
                .title("Suppress scanner traffic")
                .matchers(ImmutableList.of(Matcher.builder()
                        .type(MatcherType.USER)
                        .values(ImmutableList.of("scanner-bot"))
                        .build()))
                .build();
        assertThat(ruleWithoutId.id()).isNull();

        final EventDefinitionDto dto = baseBuilder("with-id-less-exclusion")
                .exclusions(ImmutableList.of(ruleWithoutId))
                .build();

        final EventDefinitionDto persisted = dbService.save(dto);

        assertThat(persisted.exclusions()).hasSize(1);
        assertThat(persisted.exclusions().get(0).id()).isNotBlank();
    }

    @Test
    public void preservesExistingExclusionIds() {
        final ExclusionRule ruleWithId = ExclusionRule.builder()
                .id("pre-existing-id")
                .title("Suppress scanner traffic")
                .matchers(ImmutableList.of(Matcher.builder()
                        .type(MatcherType.USER)
                        .values(ImmutableList.of("scanner-bot"))
                        .build()))
                .build();

        final EventDefinitionDto dto = baseBuilder("with-existing-id")
                .exclusions(ImmutableList.of(ruleWithId))
                .build();

        final EventDefinitionDto persisted = dbService.save(dto);

        assertThat(persisted.exclusions()).hasSize(1);
        assertThat(persisted.exclusions().get(0).id()).isEqualTo("pre-existing-id");
    }

    private EventDefinitionDto.Builder baseBuilder(String title) {
        return EventDefinitionDto.builder()
                .title(title)
                .description("test")
                .priority(1)
                .alert(false)
                .keySpec(ImmutableList.of())
                .config(TestEventProcessorConfig.builder()
                        .message("test")
                        .searchWithinMs(1000)
                        .executeEveryMs(1000)
                        .build())
                .notificationSettings(EventNotificationSettings.withGracePeriod(0))
                .tags(ImmutableSet.of());
    }
}
