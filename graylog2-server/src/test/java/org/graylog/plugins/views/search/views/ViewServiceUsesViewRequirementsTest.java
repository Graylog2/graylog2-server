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
import com.google.common.collect.ImmutableSet;
import org.graylog.security.entities.EntityOwnershipService;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.search.SearchQueryParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ViewServiceUsesViewRequirementsTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private ClusterConfigService clusterConfigService;

    private final SearchQueryParser searchQueryParser = new SearchQueryParser(ViewDTO.FIELD_TITLE, Collections.emptyMap());

    class MongoJackObjectMapperProviderForTest extends MongoJackObjectMapperProvider {
        public MongoJackObjectMapperProviderForTest(ObjectMapper objectMapper) {
            super(objectMapper);
        }

        @Override
        public ObjectMapper get() {
            return super.get().registerModule(new Jdk8Module());
        }
    }

    @Mock
    private ViewRequirements.Factory viewRequirementsFactory;

    @Mock
    private ViewRequirements viewRequirements;

    private ViewService viewService;

    @Before
    public void setUp() throws Exception {
        final MongoJackObjectMapperProvider objectMapperProvider = new MongoJackObjectMapperProviderForTest(new ObjectMapper());
        this.viewService = new ViewService(
                mongodb.mongoConnection(),
                objectMapperProvider,
                clusterConfigService,
                viewRequirementsFactory,
                mock(EntityOwnershipService.class));
        when(viewRequirementsFactory.create(any(ViewDTO.class))).then(invocation -> new ViewRequirements(Collections.emptySet(), invocation.getArgument(0)));
    }

    @After
    public void tearDown() {
        mongodb.mongoConnection().getMongoDatabase().drop();
    }

    @Test
    public void saveGetsViewRequirements() {
        final ViewDTO dto1 = ViewDTO.builder()
                .title("View 1")
                .summary("This is a nice view")
                .description("This contains lots of descriptions for the view.")
                .searchId("abc123")
                .properties(ImmutableSet.of("read-only"))
                .state(Collections.emptyMap())
                .owner("peter")
                .build();

        viewService.save(dto1);

        verify(viewRequirementsFactory, times(1)).create(dto1);
    }

    @Test
    public void updateGetsViewRequirements() {
        final ViewDTO dto1 = ViewDTO.builder()
                .id("5cee4e458fb0b6ba14faa203")
                .title("View 1")
                .summary("This is a nice view")
                .description("This contains lots of descriptions for the view.")
                .searchId("abc123")
                .properties(ImmutableSet.of("read-only"))
                .state(Collections.emptyMap())
                .owner("peter")
                .build();

        viewService.update(dto1);

        verify(viewRequirementsFactory, times(1)).create(dto1);
    }

    @Test
    @MongoDBFixtures("views.json")
    public void getReturnsViewWithViewRequirements() {
        final ViewDTO view = viewService.get("5ced4a4485b52a86b96a0a63")
                .orElseThrow(() -> new IllegalStateException("This should not happen!"));

        verify(viewRequirementsFactory, times(1)).create(view);
    }

    @Test
    @MongoDBFixtures("views.json")
    public void getDefaultReturnsViewWithViewRequirements() {
        when(clusterConfigService.get(ViewClusterConfig.class)).thenReturn(
                ViewClusterConfig.builder().defaultViewId("5ced4df1d6e8104c16f50e00").build()
        );

        final ViewDTO defaultView = viewService.getDefault()
                .orElseThrow(() -> new IllegalStateException("This should not happen!"));

        verify(viewRequirementsFactory, times(1)).create(defaultView);
    }

    @Test
    @MongoDBFixtures("views.json")
    public void forSearchReturnsViewWithViewRequirements() {
        final ViewDTO view = new ArrayList<>(viewService.forSearch("5ced4deed6e8104c16f50df9")).get(0);

        verify(viewRequirementsFactory, times(1)).create(view);
    }

    @Test
    @MongoDBFixtures("views.json")
    public void searchPaginatedReturnsViewWithViewRequirements() {
        final PaginatedList<ViewDTO> views = viewService.searchPaginated(
                searchQueryParser.parse("*"),
                view -> true,
                "desc",
                "title",
                1,
                5
        );

        final ArgumentCaptor<ViewDTO> captor = ArgumentCaptor.forClass(ViewDTO.class);
        verify(viewRequirementsFactory, times(views.delegate().size())).create(captor.capture());

        assertThat(captor.getAllValues()).isEqualTo(views.delegate());
    }

    @Test
    @MongoDBFixtures("views.json")
    public void streamAllReturnsViewWithViewRequirements() {
        final List<ViewDTO> views = viewService.streamAll().collect(Collectors.toList());

        final ArgumentCaptor<ViewDTO> captor = ArgumentCaptor.forClass(ViewDTO.class);
        verify(viewRequirementsFactory, times(views.size())).create(captor.capture());

        assertThat(captor.getAllValues()).isEqualTo(views);
    }

    @Test
    @MongoDBFixtures("views.json")
    public void streamByIdsReturnsViewWithViewRequirements() {
        final List<ViewDTO> views = viewService.streamByIds(
                ImmutableSet.of("5ced4df1d6e8104c16f50e00", "5ced4a4485b52a86b96a0a63")
        ).collect(Collectors.toList());

        final ArgumentCaptor<ViewDTO> captor = ArgumentCaptor.forClass(ViewDTO.class);
        verify(viewRequirementsFactory, times(views.size())).create(captor.capture());

        assertThat(captor.getAllValues()).isEqualTo(views);
    }
}
