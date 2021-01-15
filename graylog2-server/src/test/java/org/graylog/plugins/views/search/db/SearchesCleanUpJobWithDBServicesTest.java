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
package org.graylog.plugins.views.search.db;

import org.graylog.plugins.views.search.SearchRequirements;
import org.graylog.plugins.views.search.views.ViewRequirements;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog.security.entities.EntityOwnershipService;
import org.graylog.testing.inject.TestPasswordSecretModule;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.shared.bindings.ObjectMapperModule;
import org.graylog2.shared.bindings.ValidatorModule;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.Duration;
import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(JukitoRunner.class)
@UseModules({ObjectMapperModule.class, ValidatorModule.class, TestPasswordSecretModule.class})
public class SearchesCleanUpJobWithDBServicesTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    private SearchesCleanUpJob searchesCleanUpJob;
    private SearchDbService searchDbService;

    static class TestViewService extends ViewService {
        TestViewService(MongoConnection mongoConnection,
                        MongoJackObjectMapperProvider mapper,
                        ClusterConfigService clusterConfigService) {
            super(mongoConnection, mapper, clusterConfigService, view -> new ViewRequirements(Collections.emptySet(), view), mock(EntityOwnershipService.class));
        }
    }

    @Before
    public void setup(MongoJackObjectMapperProvider mapperProvider) {
        DateTimeUtils.setCurrentMillisFixed(DateTime.parse("2018-07-03T13:37:42.000Z").getMillis());

        final ClusterConfigService clusterConfigService = mock(ClusterConfigService.class);
        final ViewService viewService = new TestViewService(
                mongodb.mongoConnection(),
                mapperProvider,
                clusterConfigService
        );
        this.searchDbService = spy(
                new SearchDbService(
                        mongodb.mongoConnection(),
                        mapperProvider,
                        dto -> new SearchRequirements(Collections.emptySet(), dto)
                )
        );
        this.searchesCleanUpJob = new SearchesCleanUpJob(viewService, searchDbService, Duration.standardDays(4));
    }

    @After
    public void tearDown() throws Exception {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void testForAllEmpty() {
        this.searchesCleanUpJob.doRun();

        verify(searchDbService, never()).delete(any());
    }

    @Test
    @MongoDBFixtures("mixedExpiredAndNonExpiredSearches.json")
    public void testMixedExpiredAndNonExpiredSearches() {
        this.searchesCleanUpJob.doRun();

        final ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        verify(searchDbService, times(1)).delete(idCaptor.capture());

        assertThat(idCaptor.getAllValues()).containsExactly("5b3b44ca77196aa4679e4da0");
    }

    @Test
    @MongoDBFixtures("mixedExpiredNonExpiredReferencedAndNonReferencedSearches.json")
    public void testMixedExpiredNonExpiredReferencedAndNonReferencedSearches() {
        this.searchesCleanUpJob.doRun();

        final ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        verify(searchDbService, times(2)).delete(idCaptor.capture());

        assertThat(idCaptor.getAllValues()).containsExactly("5b3b44ca77196aa4679e4da1", "5b3b44ca77196aa4679e4da2");
    }

}
