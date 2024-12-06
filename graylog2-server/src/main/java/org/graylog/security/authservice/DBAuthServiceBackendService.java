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
package org.graylog.security.authservice;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import com.google.errorprone.annotations.MustBeClosed;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import jakarta.inject.Inject;
import org.bson.conversions.Bson;
import org.graylog.security.events.AuthServiceBackendDeletedEvent;
import org.graylog.security.events.AuthServiceBackendSavedEvent;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.pagination.MongoPaginationHelper;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.rest.PaginationParameters;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryParser;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.graylog2.database.utils.MongoUtils.stringIdsIn;

public class DBAuthServiceBackendService {
    public static final String COLLECTION_NAME = "auth_service_backends";

    private final Map<String, AuthServiceBackend.Factory<? extends AuthServiceBackend>> backendFactories;
    private final EventBus eventBus;
    private final ClusterEventBus clusterEventBus;
    private static final Set<String> ALLOWED_FIELDS = ImmutableSet.of(AuthServiceBackendDTO.FIELD_TITLE, AuthServiceBackendDTO.FIELD_DESCRIPTION);
    private final SearchQueryParser searchQueryParser;

    private final MongoCollection<AuthServiceBackendDTO> collection;
    private final MongoUtils<AuthServiceBackendDTO> mongoUtils;
    private final MongoPaginationHelper<AuthServiceBackendDTO> paginationHelper;

    @Inject
    protected DBAuthServiceBackendService(MongoCollections mongoCollections,
                                          Map<String, AuthServiceBackend.Factory<? extends AuthServiceBackend>> backendFactories,
                                          EventBus eventBus,
                                          ClusterEventBus clusterEventBus) {
        this.backendFactories = backendFactories;
        this.eventBus = eventBus;
        this.clusterEventBus = clusterEventBus;
        this.searchQueryParser = new SearchQueryParser(AuthServiceBackendDTO.FIELD_TITLE, ALLOWED_FIELDS);

        collection = mongoCollections.collection(COLLECTION_NAME, AuthServiceBackendDTO.class);
        mongoUtils = mongoCollections.utils(collection);
        paginationHelper = mongoCollections.paginationHelper(collection);
    }

    public AuthServiceBackendDTO save(AuthServiceBackendDTO newBackend) {
        AuthServiceBackendDTO authServiceBackendDTO = mongoUtils.save(prepareUpdate(newBackend));
        clusterEventBus.post(AuthServiceBackendSavedEvent.create(authServiceBackendDTO.id()));
        return authServiceBackendDTO;
    }

    public int delete(String id) {
        checkArgument(isNotBlank(id), "id cannot be blank");
        final int delete = mongoUtils.deleteById(id) ? 1 : 0;
        if (delete > 0) {
            eventBus.post(AuthServiceBackendDeletedEvent.create(id));
        }
        return delete;
    }

    private AuthServiceBackendDTO prepareUpdate(AuthServiceBackendDTO newBackend) {
        if (newBackend.id() == null) {
            // It's not an update
            return newBackend;
        }
        final AuthServiceBackendDTO existingBackend = get(newBackend.id())
                .orElseThrow(() -> new IllegalArgumentException("Couldn't find backend <" + newBackend.id() + ">"));

        // Call AuthServiceBackend#prepareConfigUpdate to give the backend implementation a chance to modify it
        // (e.g. handling password updates via EncryptedValue)
        return Optional.ofNullable(backendFactories.get(existingBackend.config().type()))
                .map(factory -> factory.create(existingBackend))
                .map(backend -> backend.prepareConfigUpdate(existingBackend, newBackend))
                .orElseThrow(() -> new IllegalArgumentException("Couldn't find backend implementation for type <" + existingBackend.config().type() + ">"));
    }

    public Optional<AuthServiceBackendDTO> get(String id) {
        return mongoUtils.getById(id);
    }

    public long countBackends() {
        return collection.countDocuments();
    }

    public PaginatedList<AuthServiceBackendDTO> findPaginated(PaginationParameters params,
                                                              Predicate<AuthServiceBackendDTO> filter) {

        final String sortBy = defaultIfBlank(params.getSortBy(), "title");
        final Bson sort = SortOrder.fromString(params.getOrder()).toBsonSort(sortBy);

        final String query = params.getQuery();
        Bson dbQuery = Filters.empty();
        if (!Strings.isNullOrEmpty(query)) {
            final SearchQuery searchQuery = searchQueryParser.parse(query);
            dbQuery = searchQuery.toBson();
        }

        return paginationHelper
                .filter(dbQuery)
                .sort(sort)
                .perPage(params.getPerPage())
                .page(params.getPage(), filter);
    }

    @MustBeClosed
    public Stream<AuthServiceBackendDTO> streamAll() {
        return MongoUtils.stream(collection.find());
    }

    public Stream<AuthServiceBackendDTO> streamByIds(Set<String> idSet) {
        return MongoUtils.stream(collection.find(stringIdsIn(idSet)));
    }
}
