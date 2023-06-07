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
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.dbcatalog.DbEntitiesCatalog;
import org.graylog2.database.dbcatalog.DbEntityCatalogEntry;
import org.graylog2.rest.resources.system.contentpacks.titles.model.EntitiesTitleResponse;
import org.graylog2.rest.resources.system.contentpacks.titles.model.EntityIdentifier;
import org.graylog2.rest.resources.system.contentpacks.titles.model.EntityTitleRequest;
import org.graylog2.rest.resources.system.contentpacks.titles.model.EntityTitleResponse;
import org.graylog2.streams.StreamImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.rest.resources.system.contentpacks.titles.EntityTitleServiceImpl.TITLE_IF_NOT_PERMITTED;


public class EntityTitleServiceMongoTest {

    private EntityTitleService toTest;

    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @Before
    public void setUp() {
        DbEntitiesCatalog entitiesCatalog = new DbEntitiesCatalog(
                List.of(
                        new DbEntityCatalogEntry("streams", "title", StreamImpl.class, "streams:read"),
                        new DbEntityCatalogEntry("nodes", "node_id", StreamImpl.class, "nodes:read")
                )
        );
        mongodb.start();
        mongodb.importFixture("fixture_for_title_retrieval_testing.json", EntityTitleServiceImpl.class);

        final MongoConnection connection = mongodb.mongoConnection();

        toTest = new EntityTitleServiceImpl(connection, entitiesCatalog);
    }

    @Test
    public void retrievesProperTitles() {
        //Ask for titles of 3 streams and 3 nodes
        EntityTitleRequest request = new EntityTitleRequest(
                List.of(
                        new EntityIdentifier("01020302e16f9a1d1f6b074a", "streams"),
                        new EntityIdentifier("01020302e16f9a1d1f6b074b", "streams"),
                        new EntityIdentifier("01020302e16f9a1d1f6b074c", "streams"),
                        new EntityIdentifier("01020302e16f9a1d1f6b0741", "nodes"),
                        new EntityIdentifier("01020302e16f9a1d1f6b0742", "nodes"),
                        new EntityIdentifier("01020302e000000000000000", "nodes")  //not existing one
                )
        );

        //user has permission for 2 streams and 1 nodes
        final SearchUser searchUser = TestSearchUser.builder()
                .allowStream("01020302e16f9a1d1f6b074a")
                .allowStream("01020302e16f9a1d1f6b074b")
                .allowNodeRead("01020302e16f9a1d1f6b0741")
                .build();

        final EntitiesTitleResponse response = toTest.getTitles(request, searchUser);

        assertThat(response.entities())
                .isNotNull()
                .hasSize(5)
                .contains(new EntityTitleResponse("01020302e16f9a1d1f6b074a", "streams", "Stream 1"))
                .contains(new EntityTitleResponse("01020302e16f9a1d1f6b074b", "streams", "Stream 2"))
                .contains(new EntityTitleResponse("01020302e16f9a1d1f6b074c", "streams", TITLE_IF_NOT_PERMITTED)) //not permitted
                .contains(new EntityTitleResponse("01020302e16f9a1d1f6b0741", "nodes", "Node Id 1"))
                .contains(new EntityTitleResponse("01020302e16f9a1d1f6b0742", "nodes", TITLE_IF_NOT_PERMITTED)); //not permitted

        assertThat(response.notPermitted())
                .isNotNull()
                .hasSize(2)
                .containsAll(List.of("01020302e16f9a1d1f6b074c", "01020302e16f9a1d1f6b0742"));

    }
}
