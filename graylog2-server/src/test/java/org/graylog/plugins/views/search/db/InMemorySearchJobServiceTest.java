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

import jakarta.ws.rs.NotAuthorizedException;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.plugin.system.NodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class InMemorySearchJobServiceTest {

    private SearchJobService toTest;

    @Mock
    private NodeId nodeId;
    @Mock
    private Search search;

    @BeforeEach
    public void setup() throws Exception {
        toTest = new InMemorySearchJobService(nodeId);
    }

    @Test
    public void testUsersCanLoadTheirOwnJobs() {
        final SearchJob jannettesJob = toTest.create(search, "Jannette");
        final Optional<SearchJob> retrievedJob = toTest.load(jannettesJob.getId(), mockSearchUser("Jannette"));
        assertTrue(retrievedJob.isPresent());
        assertEquals(jannettesJob, retrievedJob.get());
    }

    @Test
    public void testThrowsExceptionWhenTryingToLoadJobOfDifferentUser() {
        final SearchJob jannettesJob = toTest.create(search, "Jannette");
        assertThrows(NotAuthorizedException.class, () -> toTest.load(jannettesJob.getId(), mockSearchUser("Michelle")));
    }

    @Test
    public void testAdminCanLoadJobOfDifferentUser() {
        final SearchJob jannettesJob = toTest.create(search, "Jannette");
        final Optional<SearchJob> retrievedJob = toTest.load(jannettesJob.getId(), mockAdminSearchUser("Clara"));
        assertTrue(retrievedJob.isPresent());
        assertEquals(jannettesJob, retrievedJob.get());
    }

    @Test
    public void testReturnsEmptyOptionalWhenTryingToLoadNonExistingJob() {
        final Optional<SearchJob> retrievedJob = toTest.load("Guadalajara!", null);
        assertTrue(retrievedJob.isEmpty());
    }

    private SearchUser mockSearchUser(final String username) {
        final SearchUser searchUser = mock(SearchUser.class);
        doReturn(username).when(searchUser).username();
        return searchUser;
    }

    private SearchUser mockAdminSearchUser(final String username) {
        final SearchUser searchUser = mockSearchUser(username);
        doReturn(true).when(searchUser).isAdmin();
        return searchUser;
    }

}
