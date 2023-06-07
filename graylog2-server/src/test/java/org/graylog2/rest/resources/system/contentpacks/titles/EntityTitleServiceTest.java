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
package org.graylog2.rest.resources.system.contentpacks.titles;

import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.TestSearchUser;
import org.graylog2.database.DbEntity;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.dbcatalog.DbEntitiesCatalog;
import org.graylog2.database.dbcatalog.DbEntityCatalogEntry;
import org.graylog2.rest.resources.system.contentpacks.titles.model.EntitiesTitleResponse;
import org.graylog2.rest.resources.system.contentpacks.titles.model.EntityIdentifier;
import org.graylog2.rest.resources.system.contentpacks.titles.model.EntityTitleRequest;
import org.graylog2.rest.resources.system.contentpacks.titles.model.EntityTitleResponse;
import org.graylog2.streams.StreamImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.graylog2.rest.resources.system.contentpacks.titles.model.EntitiesTitleResponse.EMPTY_RESPONSE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit test for {@link EntityTitleServiceImpl}.
 * Tests corner cases, more complex scenario is tested against MongoDB in {@link  EntityTitleServiceMongoTest}
 */
@ExtendWith(MockitoExtension.class)
class EntityTitleServiceTest {

    private EntityTitleService toTest;

    @Mock
    private MongoConnection mongoConnection;
    @Mock
    private DbEntitiesCatalog entitiesCatalog;

    private SearchUser searchUser = TestSearchUser.builder().build();

    @BeforeEach
    void setUp() {
        toTest = new EntityTitleServiceImpl(mongoConnection, entitiesCatalog);
    }

    @Test
    void returnsEmptyResponseOnNullRequest() {
        final EntitiesTitleResponse response = toTest.getTitles(null, searchUser);
        assertSame(EMPTY_RESPONSE, response);
        verifyNoInteractions(mongoConnection);
        verifyNoInteractions(entitiesCatalog);
    }

    @Test
    void returnsEmptyResponseOnRequestWithNoEntities() {
        final EntitiesTitleResponse response = toTest.getTitles(new EntityTitleRequest(List.of()), searchUser);
        assertSame(EMPTY_RESPONSE, response);
        verifyNoInteractions(mongoConnection);
        verifyNoInteractions(entitiesCatalog);
    }

    @Test
    void returnsEmptyResponseOnRequestWithNullEntities() {
        final EntitiesTitleResponse response = toTest.getTitles(new EntityTitleRequest(null), searchUser);
        assertSame(EMPTY_RESPONSE, response);
        verifyNoInteractions(mongoConnection);
        verifyNoInteractions(entitiesCatalog);
    }

    @Test
    void returnsEmptyResponseIfNoEntitiesArePresentInTheCatalog() {
        final EntitiesTitleResponse response = toTest.getTitles(new EntityTitleRequest(
                List.of(
                        new EntityIdentifier("1", "streams"),
                        new EntityIdentifier("2", "users")
                )
        ), searchUser);
        assertSame(EMPTY_RESPONSE, response);
        verifyNoInteractions(mongoConnection);
    }

    @Test
    void returnsResponseWithEmptyTitleIfCatalogSaysEntityTypeHasNoTitleColumn() {

        doReturn(Optional.of(
                        new DbEntityCatalogEntry(
                                "streams",
                                DbEntity.NO_TITLE,
                                StreamImpl.class,
                                DbEntity.ALL_ALLOWED)
                )
        ).when(entitiesCatalog).getByCollectionName("streams");

        final EntitiesTitleResponse response = toTest.getTitles(new EntityTitleRequest(
                List.of(
                        new EntityIdentifier("1", "streams")
                )
        ), searchUser);

        assertEquals(
                new EntitiesTitleResponse(
                        Set.of(new EntityTitleResponse("1", "streams", "")),
                        Set.of()
                ),
                response);

        verifyNoInteractions(mongoConnection);
    }
}
