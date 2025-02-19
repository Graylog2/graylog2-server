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
package org.graylog.plugins.views.search.jobs;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import jakarta.inject.Inject;
import org.bson.Document;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.utils.MongoUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Optional;
import java.util.Set;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.lte;
import static com.mongodb.client.model.Projections.include;
import static org.graylog.plugins.views.search.SearchJobIdentifier.OWNER_FIELD;
import static org.graylog.plugins.views.search.SearchJobIdentifier.SEARCH_ID_FIELD;
import static org.graylog.plugins.views.search.jobs.SearchJobState.CREATED_AT_FIELD;
import static org.graylog.plugins.views.search.jobs.SearchJobState.STATUS_FIELD;
import static org.graylog.plugins.views.search.jobs.SearchJobState.TYPE_FIELD;
import static org.graylog.plugins.views.search.jobs.SearchJobType.DATA_LAKE;

public class SearchJobStateService {

    public static final String COLLECTION_NAME = "search_job_states";

    private final MongoCollection<SearchJobState> collection;
    private final MongoUtils<SearchJobState> mongoUtils;

    @Inject
    public SearchJobStateService(final MongoCollections mongoCollections) {
        this.collection = mongoCollections.collection(COLLECTION_NAME, SearchJobState.class);
        this.mongoUtils = mongoCollections.utils(collection);
        this.collection.createIndex(Indexes.ascending(SEARCH_ID_FIELD));
        this.collection.createIndex(Indexes.ascending(OWNER_FIELD));
        this.collection.createIndex(Indexes.ascending(CREATED_AT_FIELD));
    }

    public Optional<SearchJobState> get(final String id) {
        return mongoUtils.getById(id);
    }

    public Optional<SearchJobState> getLatestForUser(final String user) {
        return Optional.ofNullable(
                collection.find(Filters.eq(OWNER_FIELD, user))
                        .sort(Sorts.descending(CREATED_AT_FIELD))
                        .first()
        );
    }

    /**
     * Reset latest/active search job for a given user.
     *
     * @param user Current user
     * @return {@link Optional<SearchJobState>} object representing state of latest/active search job before the reset.
     */
    public Optional<SearchJobState> resetLatestForUser(final String user) {
        return getLatestForUser(user).map(activeQuerySearchJobState -> {
                    update(
                            activeQuerySearchJobState.toBuilder()
                                    .status(SearchJobStatus.RESET)
                                    .result(null)
                                    .errors(Set.of())
                                    .build()
                    );
                    return activeQuerySearchJobState;
                }
        );
    }

    public boolean delete(final String id) {
        return mongoUtils.deleteById(id);
    }

    public long deleteOlderThan(final DateTime dateTime) {
        final DeleteResult deleteResult = collection.deleteMany(lte(CREATED_AT_FIELD, dateTime));
        return deleteResult.getDeletedCount();
    }

    public long expireOlderThan(final DateTime dateTime) {
        final UpdateResult updateResult = collection.updateMany(
                and(
                        eq(TYPE_FIELD, DATA_LAKE),
                        //eq(STATUS_FIELD, DONE) //TODO: should all jobs be expired, or only DONE ones?
                        lte(CREATED_AT_FIELD, dateTime)
                ),
                Updates.set(STATUS_FIELD, SearchJobStatus.EXPIRED)

        );
        return updateResult.getModifiedCount();
    }

    public SearchJobState create(final SearchJobState searchJobState) {
        final InsertOneResult insertOneResult = this.collection.insertOne(searchJobState);
        return get(MongoUtils.insertedIdAsString(insertOneResult)).orElseThrow(() -> new IllegalStateException("Unable to retrieve saved search job state!"));
    }

    public boolean update(final SearchJobState searchJobState) {
        if (searchJobState.identifier().id() == null) {
            throw new IllegalStateException("Missing ID of SearchJobState to update");
        }
        final Optional<SearchJobStatus> status = getStatus(searchJobState.identifier().id());
        if (status.isPresent() && status.get() == SearchJobStatus.RESET) {
            //RESET search jobs should not be changed anymore, are immutable
            return false;
        } else {
            final UpdateResult updateResult = collection.replaceOne(
                    MongoUtils.idEq(searchJobState.identifier().id()),
                    searchJobState.toBuilder().updatedAt(DateTime.now(DateTimeZone.UTC)).build(),
                    new ReplaceOptions().upsert(true)
            );
            return updateResult.getModifiedCount() > 0;
        }
    }

    public boolean changeStatus(final String searchJobStateID,
                                final SearchJobStatus searchJobStatus) {
        return get(searchJobStateID)
                .map(searchJobState -> searchJobState.toBuilder()
                        .status(searchJobStatus)
                        .updatedAt(DateTime.now(DateTimeZone.UTC))
                        .build())
                .map(this::update)
                .orElse(false);

    }

    public Optional<SearchJobStatus> getStatus(final String searchJobStateID) {
        final Document doc = collection.find(MongoUtils.idEq(searchJobStateID), Document.class)
                .projection(include(STATUS_FIELD))
                .first();
        if (doc != null) {
            return Optional.of(SearchJobStatus.valueOf(doc.get(STATUS_FIELD, String.class)));
        } else {
            return Optional.empty();
        }
    }
}
