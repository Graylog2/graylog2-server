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

import jakarta.ws.rs.ForbiddenException;
import org.assertj.core.api.Assertions;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.TestSearchUser;
import org.graylog2.plugin.system.SimpleNodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

public class InMemorySearchJobServiceTest {

    private SearchJobService toTest;


    @BeforeEach
    public void setup() throws Exception {
        toTest = new InMemorySearchJobService(new SimpleNodeId("5ca1ab1e-0000-4000-a000-000000000000"));
    }

    @Test
    public void testUsersCanLoadTheirOwnJobs() {
        final SearchJob jannettesJob = toTest.create(Search.builder().build(), "Jannette");
        final Optional<SearchJob> retrievedJob = toTest.load(jannettesJob.getId(), searchUser("Jannette"));
        Assertions.assertThat(retrievedJob)
                .isPresent()
                .hasValue(jannettesJob);
    }

    @Test
    public void testThrowsExceptionWhenTryingToLoadJobOfDifferentUser() {
        final SearchJob jannettesJob = toTest.create(Search.builder().build(), "Jannette");
        Assertions.assertThatThrownBy(() -> toTest.load(jannettesJob.getId(), searchUser("Michelle")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    public void testAdminCanLoadJobOfDifferentUser() {
        final SearchJob jannettesJob = toTest.create(Search.builder().build(), "Jannette");
        final Optional<SearchJob> retrievedJob = toTest.load(jannettesJob.getId(), adminUser("Clara"));
        Assertions.assertThat(retrievedJob)
                .isPresent()
                .hasValue(jannettesJob);
    }

    @Test
    public void testReturnsEmptyOptionalWhenTryingToLoadNonExistingJob() {
        final Optional<SearchJob> retrievedJob = toTest.load("Guadalajara!", null);
        Assertions.assertThat(retrievedJob)
                .isEmpty();
    }

    private SearchUser searchUser(final String username) {
        return TestSearchUser.builder()
                .withUser(u -> u.withUsername(username))
                 .build();
    }

    private SearchUser adminUser(final String username) {
        return TestSearchUser.builder()
                .withUser(u -> u.withUsername(username).isLocalAdmin(true))
                .build();
    }
}
