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

import com.google.common.collect.ImmutableSet;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOptions;
import jakarta.inject.Inject;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchRequirements;
import org.graylog.plugins.views.search.SearchSummary;
import org.graylog.plugins.views.search.searchfilters.db.SearchFiltersReFetcher;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.utils.MongoUtils;
import org.joda.time.Instant;

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
    protected final MongoCollection<Search> db;
    protected final MongoCollection<SearchSummary> summarydb;
    private final SearchRequirements.Factory searchRequirementsFactory;
    private final SearchFiltersReFetcher searchFiltersRefetcher;
    private final MongoUtils<Search> mongoUtils;

    @Inject
    protected SearchDbService(MongoCollections mongoCollections,
                              SearchRequirements.Factory searchRequirementsFactory,
                              SearchFiltersReFetcher searchFiltersRefetcher) {
        this.searchRequirementsFactory = searchRequirementsFactory;
        db = mongoCollections.collection("searches", Search.class);
        db.createIndex(Indexes.ascending(Search.FIELD_CREATED_AT));
        summarydb = mongoCollections.collection("searches", SearchSummary.class);
        this.searchFiltersRefetcher = searchFiltersRefetcher;
        this.mongoUtils = mongoCollections.utils(db);
    }

    public Optional<Search> get(String id) {
        return mongoUtils.getById(id)
                .map(this::getSearchWithRefetchedFilters)
                .map(this::requirementsForSearch);
    }

    private Search getSearchWithRefetchedFilters(final Search search) {
        if (searchFiltersRefetchNeeded(search)) {
            return search.toBuilder()
                    .queries(search.queries()
                            .stream()
                            .map(query -> query.toBuilder()
                                    .filters(searchFiltersRefetcher.reFetch(query.filters()))
                                    .build())
                            .collect(ImmutableSet.toImmutableSet())
                    )
                    .build();
        } else {
            return search;
        }
    }

    private boolean searchFiltersRefetchNeeded(Search search) {
        return searchFiltersRefetcher.turnedOn() &&
                search.queries()
                        .stream()
                        .anyMatch(Query::hasReferencedStreamFilters);
    }

    public Search save(Search search) {
        final Search searchToSave = requirementsForSearch(search);
        if (searchToSave.id() != null) {
            db.replaceOne(
                    MongoUtils.idEq(search.id()),
                    searchToSave,
                    new ReplaceOptions().upsert(true)
            );

            return searchToSave;
        }

        final var save = db.insertOne(searchToSave);

        return get(MongoUtils.insertedIdAsString(save)).orElseThrow(() -> new IllegalStateException("Unable to retrieve saved search!"));
    }

    /**
     * Searches should only be deleted directly by {@link SearchesCleanUpJob} if they are no longer referenced
     * by any views. Do not directly delete searches when deleting views. The searches might still be referenced by
     * other view copies (until those copies are modified, at which time a new search would be created).
     *
     * @param id A Search ID.
     */
    void delete(String id) {
        mongoUtils.getById(id);
    }

    public Collection<Search> findByIds(Set<String> idSet) {
        return MongoUtils.stream(db.find(MongoUtils.stringIdsIn(idSet)))
                .map(this::getSearchWithRefetchedFilters)
                .map(this::requirementsForSearch)
                .collect(Collectors.toList());
    }

    public Stream<Search> streamAll() {
        return MongoUtils.stream(db.find())
                .map(this::getSearchWithRefetchedFilters)
                .map(this::requirementsForSearch);
    }

    private Search requirementsForSearch(Search search) {
        return searchRequirementsFactory.create(search)
                .rebuildRequirements(Search::requires, (s, newRequirements) -> s.toBuilder().requires(newRequirements).build());
    }

    Stream<SearchSummary> findSummaries() {
        return MongoUtils.stream(summarydb.find());
    }

    public Set<String> getExpiredSearches(final Set<String> neverDeleteIds, final Instant mustBeOlderThan) {
        return this.findSummaries()
                .filter(search -> !neverDeleteIds.contains(search.id()) && search.createdAt().isBefore(mustBeOlderThan))
                .map(SearchSummary::id)
                .collect(Collectors.toSet());
    }
}
