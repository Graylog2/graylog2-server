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
package org.graylog.plugins.views.search.views;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.graylog.security.entities.EntityOwnershipService;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.cluster.ClusterConfigServiceImpl;
import org.graylog2.database.PaginatedList;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;
import org.graylog2.shared.plugins.ChainingClassLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class ViewServiceTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    private ViewService dbService;
    private ClusterConfigServiceImpl clusterConfigService;

    class MongoJackObjectMapperProviderForTest extends MongoJackObjectMapperProvider {
        public MongoJackObjectMapperProviderForTest(ObjectMapper objectMapper) {
            super(objectMapper);
        }

        @Override
        public ObjectMapper get() {
            return super.get().registerModule(new Jdk8Module());
        }
    }

    @Before
    public void setUp() throws Exception {
        final MongoJackObjectMapperProvider objectMapperProvider = new MongoJackObjectMapperProviderForTest(new ObjectMapper());
        this.clusterConfigService = new ClusterConfigServiceImpl(
                objectMapperProvider,
                mongodb.mongoConnection(),
                mock(NodeId.class),
                new ChainingClassLoader(getClass().getClassLoader()),
                new ClusterEventBus()
        );
        this.dbService = new ViewService(
                mongodb.mongoConnection(),
                objectMapperProvider,
                clusterConfigService,
                view -> new ViewRequirements(Collections.emptySet(), view),
                mock(EntityOwnershipService.class));

    }

    @After
    public void tearDown() {
        mongodb.mongoConnection().getMongoDatabase().drop();
    }

    private void hasValidId(ViewDTO dto) {
        assertThat(dto.id())
                .isNotNull()
                .matches("^[a-z0-9]{24}");
    }

    @Test
    public void crud() {
        final ViewDTO dto1 = ViewDTO.builder()
                .title("View 1")
                .summary("This is a nice view")
                .description("This contains lots of descriptions for the view.")
                .searchId("abc123")
                .properties(ImmutableSet.of("read-only"))
                .state(Collections.emptyMap())
                .owner("peter")
                .build();
        final ViewDTO dto2 = ViewDTO.builder()
                .title("View 2")
                .searchId("abc123")
                .state(Collections.emptyMap())
                .owner("paul")
                .build();

        final ViewDTO savedDto1 = dbService.save(dto1);
        final ViewDTO savedDto2 = dbService.save(dto2);

        assertThat(savedDto1)
                .satisfies(this::hasValidId)
                .extracting("title", "summary", "description", "searchId", "properties")
                .containsExactly("View 1", "This is a nice view", "This contains lots of descriptions for the view.", "abc123", ImmutableSet.of("read-only"));

        assertThat(savedDto2)
                .satisfies(this::hasValidId)
                .extracting("title", "summary", "description", "searchId", "properties")
                .containsExactly("View 2", "", "", "abc123", ImmutableSet.of());

        assertThat(dbService.get(savedDto1.id()))
                .isPresent()
                .get()
                .extracting("title")
                .containsExactly("View 1");

        dbService.delete(savedDto2.id());

        assertThat(dbService.get(savedDto2.id())).isNotPresent();
    }

    @Test
    public void searchPaginated() {
        final ImmutableMap<String, SearchQueryField> searchFieldMapping = ImmutableMap.<String, SearchQueryField>builder()
                .put("id", SearchQueryField.create(ViewDTO.FIELD_ID))
                .put("title", SearchQueryField.create(ViewDTO.FIELD_TITLE))
                .put("summary", SearchQueryField.create(ViewDTO.FIELD_DESCRIPTION))
                .build();

        dbService.save(ViewDTO.builder().title("View A").searchId("abc123").state(Collections.emptyMap()).owner("franz").build());
        dbService.save(ViewDTO.builder().title("View B").searchId("abc123").state(Collections.emptyMap()).owner("franz").build());
        dbService.save(ViewDTO.builder().title("View C").searchId("abc123").state(Collections.emptyMap()).owner("franz").build());
        dbService.save(ViewDTO.builder().title("View D").searchId("abc123").state(Collections.emptyMap()).owner("franz").build());
        dbService.save(ViewDTO.builder().title("View E").searchId("abc123").state(Collections.emptyMap()).owner("franz").build());

        final SearchQueryParser queryParser = new SearchQueryParser(ViewDTO.FIELD_TITLE, searchFieldMapping);

        final PaginatedList<ViewDTO> result1 = dbService.searchPaginated(
                queryParser.parse("A B D"),
                view -> true, "desc",
                "title",
                1,
                5
        );

        assertThat(result1)
                .hasSize(3)
                .extracting("title")
                .containsExactly("View D", "View B", "View A");

        final PaginatedList<ViewDTO> result2 = dbService.searchPaginated(
                queryParser.parse("A B D"),
                view -> view.title().contains("B") || view.title().contains("D"), "desc",
                "title",
                1,
                5
        );

        assertThat(result2)
                .hasSize(2)
                .extracting("title")
                .containsExactly("View D", "View B");
    }

    @Test
    public void saveAndGetDefault() {
        dbService.save(ViewDTO.builder().title("View A").searchId("abc123").state(Collections.emptyMap()).owner("hans").build());
        final ViewDTO savedView2 = dbService.save(ViewDTO.builder().title("View B").searchId("abc123").state(Collections.emptyMap()).owner("hans").build());

        dbService.saveDefault(savedView2);

        assertThat(dbService.getDefault())
                .isPresent()
                .get()
                .extracting("id", "title")
                .containsExactly(savedView2.id(), "View B");

        assertThatThrownBy(() -> dbService.saveDefault(ViewDTO.builder().title("err").searchId("abc123").state(Collections.emptyMap()).build()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
