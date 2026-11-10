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
import com.mongodb.client.model.Filters;
import org.bson.types.ObjectId;
import org.graylog.events.TestEventProcessorConfig;
import org.graylog.events.notifications.EventNotificationSettings;
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
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MongoDBExtension.class)
public class DBEventDefinitionServiceSuggestTagsTest {
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
    public void emptyCollectionReturnsEmptyList() {
        assertThat(dbService.suggestTags(null, 100)).isEmpty();
    }

    @Test
    public void distinctSortedUnionAcrossEventDefinitions() {
        save("a", ImmutableSet.of("phishing", "lateral-movement"));
        save("b", ImmutableSet.of("phishing", "exfiltration"));
        save("c", ImmutableSet.of("persistence"));

        assertThat(dbService.suggestTags(null, 100))
                .containsExactly("exfiltration", "lateral-movement", "persistence", "phishing");
    }

    @Test
    public void substringMatchIsCaseInsensitive() {
        save("a", ImmutableSet.of("phishing"));
        save("b", ImmutableSet.of("persistence"));

        assertThat(dbService.suggestTags("ph", 100)).containsExactly("phishing");
        assertThat(dbService.suggestTags("PH", 100)).containsExactly("phishing");
    }

    @Test
    public void substringMatchesAnywhereInTheTag() {
        save("a", ImmutableSet.of("phishing", "lateral-movement", "persistence"));

        // "ish" appears in the middle of "phishing"; "rsi" inside "persistence".
        assertThat(dbService.suggestTags("ish", 100)).containsExactly("phishing");
        assertThat(dbService.suggestTags("rsi", 100)).containsExactly("persistence");
    }

    @Test
    public void substringWithRegexMetaIsEscaped() {
        save("a", ImmutableSet.of("phishing", "lateral-movement", "persistence"));

        assertThat(dbService.suggestTags("*", 100)).isEmpty();
        assertThat(dbService.suggestTags("[a-z]", 100)).isEmpty();
        assertThat(dbService.suggestTags(".", 100)).isEmpty();
    }

    @Test
    public void limitIsApplied() {
        save("a", ImmutableSet.of("a-tag", "b-tag", "c-tag", "d-tag"));

        assertThat(dbService.suggestTags(null, 2)).containsExactly("a-tag", "b-tag");
    }

    @Test
    public void blankPrefixIsTreatedAsNoFilter() {
        save("a", ImmutableSet.of("phishing"));

        assertThat(dbService.suggestTags("", 100)).containsExactly("phishing");
        assertThat(dbService.suggestTags("   ", 100)).containsExactly("phishing");
    }

    @Test
    public void restrictedToPermittedIdsNarrowsResults() {
        final String idA = save("a", ImmutableSet.of("phishing", "lateral-movement"));
        save("b", ImmutableSet.of("exfiltration"));

        assertThat(dbService.suggestTags(null, 100, List.of(new ObjectId(idA))))
                .containsExactly("lateral-movement", "phishing");
    }

    @Test
    public void emptyPermittedIdsReturnsEmpty() {
        save("a", ImmutableSet.of("phishing"));

        assertThat(dbService.suggestTags(null, 100, List.of())).isEmpty();
    }

    @Test
    public void findPermittedIdsRespectsPredicate() {
        final String idA = save("a", ImmutableSet.of("phishing"));
        final String idB = save("b", ImmutableSet.of("exfiltration"));

        // Predicate that only permits idA.
        final var permitted = dbService.findPermittedIds(idA::equals);
        assertThat(permitted).containsExactly(new ObjectId(idA));

        // Predicate that permits everyone.
        assertThat(dbService.findPermittedIds(id -> true))
                .containsExactlyInAnyOrder(new ObjectId(idA), new ObjectId(idB));
    }

    private String save(String title, ImmutableSet<String> tags) {
        final EventDefinitionDto dto = EventDefinitionDto.builder()
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
                .tags(tags)
                .build();
        return dbService.save(dto).id();
    }
}
