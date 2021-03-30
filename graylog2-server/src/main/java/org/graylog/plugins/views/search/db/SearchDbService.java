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

import com.google.common.collect.Streams;
import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchRequirements;
import org.graylog.plugins.views.search.SearchSummary;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedList;
import org.joda.time.Instant;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is a helper to implement a basic Mongojack-based database service that allows CRUD operations on a single DTO type.
 *
 * <p>
 * Subclasses can add more sophisticated search methods by access the protected "db" property.<br/>
 * Indices can be added in the constructor.
 * </p>
 */
public class SearchDbService {
    protected final JacksonDBCollection<Search, ObjectId> db;
    protected final JacksonDBCollection<SearchSummary, ObjectId> summarydb;
    private final SearchRequirements.Factory searchRequirementsFactory;

    @Inject
    protected SearchDbService(MongoConnection mongoConnection,
                              MongoJackObjectMapperProvider mapper,
                              SearchRequirements.Factory searchRequirementsFactory) {
        this.searchRequirementsFactory = searchRequirementsFactory;
        db = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection("searches"),
                Search.class,
                ObjectId.class,
                mapper.get());
        db.createIndex(new BasicDBObject("created_at", 1), new BasicDBObject("unique", false));
        summarydb = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection("searches"),
                SearchSummary.class,
                ObjectId.class,
                mapper.get());
    }

    public Optional<Search> get(String id) {
        return Optional.ofNullable(db.findOneById(new ObjectId(id)))
                .map(this::requirementsForSearch);
    }

    public Search save(Search search) {
        final Search searchToSave = requirementsForSearch(search);
        if (searchToSave.id() != null) {
            db.update(
                    DBQuery.is("_id", search.id()),
                    searchToSave,
                    true,
                    false
            );

            return searchToSave;
        }

        final WriteResult<Search, ObjectId> save = db.insert(searchToSave);

        return save.getSavedObject();
    }

    public PaginatedList<Search> findPaginated(DBQuery.Query search, DBSort.SortBuilder sort, int page, int perPage) {

        final DBCursor<Search> cursor = db.find(search)
                .sort(sort)
                .limit(perPage)
                .skip(perPage * Math.max(0, page - 1));

        return new PaginatedList<>(
                Streams.stream((Iterable<Search>) cursor).map(this::requirementsForSearch).collect(Collectors.toList()),
                cursor.count(),
                page,
                perPage
        );
    }

    public void delete(String id) {
        db.removeById(new ObjectId(id));
    }

    public Collection<Search> findByIds(Set<String> idSet) {
        return Streams.stream((Iterable<Search>) db.find(DBQuery.in("_id", idSet.stream().map(ObjectId::new).collect(Collectors.toList()))))
                .map(this::requirementsForSearch)
                .collect(Collectors.toList());
    }

    public Stream<Search> streamAll() {
        return Streams.stream((Iterable<Search>) db.find()).map(this::requirementsForSearch);
    }

    private Search requirementsForSearch(Search search) {
        return searchRequirementsFactory.create(search)
                .rebuildRequirements(Search::requires, (s, newRequirements) -> s.toBuilder().requires(newRequirements).build());
    }

     public Set<String> getExpiredSearches(final Set<String> requiredIds, final Instant mustNotBeOlderThan) {
        return Streams.stream((Iterable<SearchSummary>) summarydb.find())
                .filter(search -> search.createdAt().isBefore(mustNotBeOlderThan) && !requiredIds.contains(search.id()))
                .map(search -> search.id())
                .collect(Collectors.toSet());
    }
}
