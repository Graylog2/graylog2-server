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
import com.google.common.eventbus.Subscribe;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.PushOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import jakarta.inject.Inject;
import org.graylog.plugins.views.startpage.lastOpened.LastOpenedDTO;
import org.graylog.plugins.views.startpage.lastOpened.LastOpenedForUserDTO;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.database.users.User;
import org.graylog2.users.events.UserDeletedEvent;

import java.time.Clock;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.graylog.plugins.views.search.querystrings.QueryString.FIELD_QUERY;
import static org.graylog.plugins.views.search.querystrings.QueryStringForUser.FIELD_ITEMS;
import static org.graylog.plugins.views.search.querystrings.QueryStringForUser.FIELD_USER_ID;

public class MongoLastUsedQueryStringsService implements LastUsedQueryStringsService {
    public static final String COLLECTION_NAME = "query_strings";
    private static final Integer MAX_LENGTH = 100;
    private final Integer maxLength;
    private final MongoCollection<QueryStringForUser> collection;
    private final Clock clock;

    public MongoLastUsedQueryStringsService(MongoConnection mongoConnection,
                                            MongoJackObjectMapperProvider mapper,
                                            EventBus eventBus,
                                            Integer maxLength) {
        this.collection = mongoConnection.getMongoDatabase().getCollection(COLLECTION_NAME, QueryStringForUser.class);
        this.clock = Clock.systemDefaultZone();
        this.maxLength = maxLength;

        eventBus.register(this);

        this.collection.createIndex(new BasicDBObject(LastOpenedForUserDTO.FIELD_USER_ID, 1));
        this.collection.createIndex(new BasicDBObject(LastOpenedForUserDTO.FIELD_ITEMS + "." + LastOpenedDTO.FIELD_GRN, 1));
    }

    @Inject
    public MongoLastUsedQueryStringsService(MongoConnection mongoConnection,
                                            MongoJackObjectMapperProvider mapper,
                                            EventBus eventBus) {
        this(mongoConnection, mapper, eventBus, MAX_LENGTH);
    }

    @Override
    public List<QueryString> get(User user, int limit) {
        return findForUser(user.getId())
                .map(QueryStringForUser::items)
                .map(items -> items.stream().limit(limit).toList())
                .orElse(List.of());
    }

    private Optional<QueryStringForUser> findForUser(final String userId) {
        return Optional.ofNullable(this.collection.find(Filters.eq(FIELD_USER_ID, userId), QueryStringForUser.class).first());
    }


    @Override
    public void save(User user, String queryString) {
        final var newItem = new QueryString(queryString, Date.from(clock.instant()));
        this.collection.updateOne(Filters.eq(FIELD_USER_ID, user.getId()),
                Updates.pull(FIELD_ITEMS, Filters.eq(FIELD_QUERY, queryString)));
        this.collection.updateOne(Filters.eq(FIELD_USER_ID, user.getId()),
                Updates.pushEach(FIELD_ITEMS, List.of(newItem), new PushOptions().position(0).slice(maxLength)),
                new UpdateOptions().upsert(true));
    }

    @Subscribe
    public void removeQueryStringsUponUserDeletion(final UserDeletedEvent userDeletedEvent) {
        this.collection.deleteOne(Filters.eq(FIELD_USER_ID, userDeletedEvent.userId()));
    }
}
