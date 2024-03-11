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
package org.graylog.plugins.views.search.querystrings;

import com.google.common.eventbus.EventBus;
import org.graylog.plugins.views.search.rest.TestUser;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.plugin.database.users.User;
import org.graylog2.users.events.UserDeletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
class MongoLastUsedQueryStringsServiceTest {
    private User user;
    private User admin;

    private MongoLastUsedQueryStringsService service;

    @BeforeEach
    void setUp(MongoDBTestService mongodb,
               MongoJackObjectMapperProvider mongoJackObjectMapperProvider) {

        admin = TestUser.builder().withId("637748db06e1d74da0a54331").withUsername("local:admin").isLocalAdmin(true).build();
        user = TestUser.builder().withId("637748db06e1d74da0a54330").withUsername("test").isLocalAdmin(false).build();

        this.service = new MongoLastUsedQueryStringsService(mongodb.mongoConnection(), mongoJackObjectMapperProvider, new EventBus(), 3);
    }

    @Test
    void storedQueryStringCanBeRetrieved() {
        assertThat(queryStrings(service.get(user))).isEmpty();

        service.save(user, "http_method:GET");

        assertThat(queryStrings(service.get(user))).containsExactly("http_method:GET");

        assertThat(queryStrings(service.get(admin))).isEmpty();
    }

    @Test
    void queryStringsAreScopedByUser() {
        assertThat(service.get(user)).isEmpty();

        service.save(user, "http_method:GET");

        assertThat(queryStrings(service.get(admin))).isEmpty();

        service.save(admin, "action:foo");

        assertThat(queryStrings(service.get(user))).containsExactly("http_method:GET");
    }

    @Test
    void queryStringsAreCappedAtMaximumLength() {
        service.save(user, "query string 1");
        service.save(user, "query string 2");
        service.save(user, "query string 3");
        service.save(user, "query string 4");

        assertThat(queryStrings(service.get(user))).containsExactly(
                "query string 4",
                "query string 3",
                "query string 2"
        );
    }

    @Test
    void queryStringsAreDeduplicated() {
        service.save(user, "query string 1");
        service.save(user, "query string 2");
        service.save(user, "query string 1");

        assertThat(queryStrings(service.get(user))).containsExactly(
                "query string 1",
                "query string 2"
        );
    }

    @Test
    void removeDataWhenUserIsDeleted() {
        service.save(admin, "query string 1");
        service.save(user, "query string 2");

        assertThat(queryStrings(service.get(admin))).containsExactly("query string 1");
        assertThat(queryStrings(service.get(user))).containsExactly("query string 2");

        service.removeQueryStringsUponUserDeletion(UserDeletedEvent.create(user.getId(), user.getName()));

        assertThat(queryStrings(service.get(admin))).containsExactly("query string 1");
        assertThat(queryStrings(service.get(user))).isEmpty();
    }

    private List<String> queryStrings(List<QueryString> queryStrings) {
        return queryStrings.stream().map(QueryString::query).toList();
    }
}
