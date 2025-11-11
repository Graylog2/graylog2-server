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

import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DashboardServiceTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    private DashboardService dashboardService;

    @Before
    public void setUp() {
        final MongoJackObjectMapperProvider objectMapperProvider = new MongoJackObjectMapperProvider(
                new ObjectMapperProvider().get()
        );
        final MongoCollections mongoCollections = new MongoCollections(objectMapperProvider, mongodb.mongoConnection());
        final ViewService viewService = mock(ViewService.class);

        when(viewService.collection()).thenReturn(
                mongoCollections.collection(ViewService.COLLECTION_NAME, ViewDTO.class)
        );

        dashboardService = new DashboardService(viewService);
    }

    @Test
    @MongoDBFixtures("dashboards.json")
    public void testCountBySource() {
        final Map<String, Long> counts = dashboardService.countBySource();

        assertThat(counts)
                .containsEntry("illuminate_dashboards", 1L)
                .containsEntry("user_dashboards", 1L);
    }
}
