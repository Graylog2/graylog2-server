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
package org.graylog.security.authzroles;

import com.google.common.collect.ImmutableSet;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.pagination.MongoPaginationHelper;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.search.SearchQuery;
import org.graylog2.shared.users.UserService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

public class PaginatedAuthzRolesService {
    private static final String COLLECTION_NAME = "roles";

    private final MongoCollection<AuthzRoleDTO> collection;
    private final MongoCollection<Document> documentCollection;
    private final UserService userService;

    private final MongoUtils<AuthzRoleDTO> mongoUtils;
    private final MongoPaginationHelper<AuthzRoleDTO> paginationHelper;

    @Inject
    public PaginatedAuthzRolesService(MongoCollections mongoCollections, UserService userService) {
        this.userService = userService;

        collection = mongoCollections.collection(COLLECTION_NAME, AuthzRoleDTO.class);
        mongoUtils = mongoCollections.utils(collection);
        paginationHelper = mongoCollections.paginationHelper(collection);
        documentCollection = mongoCollections.nonEntityCollection(COLLECTION_NAME, Document.class);
    }

    public long count() {
        return collection.countDocuments();
    }

    public ImmutableSet<String> getAllRoleIds() {
        // Use a projection here to avoid the mongojack deserializing and object creation overhead
        final FindIterable<Document> docs = documentCollection.find().projection(Projections.include("_id"));

        return StreamSupport.stream(docs.spliterator(), false)
                .map(doc -> doc.get("_id", ObjectId.class).toHexString())
                .collect(ImmutableSet.toImmutableSet());
    }

    public List<AuthzRoleDTO> findByIds(Collection<String> ids) {
        return collection.find(MongoUtils.stringIdsIn(ids)).into(new ArrayList<>());
    }

    /**
     * @deprecated Use {@link #findPaginated(SearchQuery, int, int, String, SortOrder)}
     */
    @Deprecated
    public PaginatedList<AuthzRoleDTO> findPaginated(SearchQuery searchQuery, int page,
                                                     int perPage, String sortField, String order) {
        return findPaginated(searchQuery, page, perPage, sortField, SortOrder.fromString(order));
    }

    public PaginatedList<AuthzRoleDTO> findPaginated(SearchQuery searchQuery, int page,
                                                     int perPage, String sortField, SortOrder order) {
        return paginationHelper
                .filter(searchQuery.toBson())
                .sort(order.toBsonSort(sortField))
                .perPage(perPage)
                .includeGrandTotal(true)
                .page(page);
    }

    /**
     * @deprecated use {@link #findPaginatedByIds(SearchQuery, int, int, String, SortOrder, Set)}
     */
    @Deprecated
    public PaginatedList<AuthzRoleDTO> findPaginatedByIds(SearchQuery searchQuery,
                                                          int page,
                                                          int perPage,
                                                          String sortField,
                                                          String order,
                                                          Set<String> roleIds) {
        return findPaginatedByIds(searchQuery, page, perPage, sortField, SortOrder.fromString(order), roleIds);
    }

    public PaginatedList<AuthzRoleDTO> findPaginatedByIds(SearchQuery searchQuery,
                                                          int page,
                                                          int perPage,
                                                          String sortField,
                                                          SortOrder order,
                                                          Set<String> roleIds) {
        return paginationHelper
                .filter(buildRoleIdsQuery(searchQuery, roleIds))
                .sort(order.toBsonSort(sortField))
                .perPage(perPage)
                .includeGrandTotal(true)
                .page(page);
    }

    /**
     * @deprecated use {@link #findPaginatedByIdsWithFilter(SearchQuery, Predicate, int, int, String, SortOrder, Set)}
     */
    @Deprecated
    public PaginatedList<AuthzRoleDTO> findPaginatedByIdsWithFilter(SearchQuery searchQuery,
                                                                    Predicate<AuthzRoleDTO> filter,
                                                                    int page,
                                                                    int perPage,
                                                                    String sortField,
                                                                    String order,
                                                                    Set<String> roleIds) {
        return findPaginatedByIdsWithFilter(searchQuery, filter, page, perPage, sortField, SortOrder.fromString(order),
                roleIds);

    }

    public PaginatedList<AuthzRoleDTO> findPaginatedByIdsWithFilter(SearchQuery searchQuery,
                                                                    Predicate<AuthzRoleDTO> filter,
                                                                    int page,
                                                                    int perPage,
                                                                    String sortField,
                                                                    SortOrder order,
                                                                    Set<String> roleIds) {
        return paginationHelper
                .filter(buildRoleIdsQuery(searchQuery, roleIds))
                .sort(order.toBsonSort(sortField))
                .perPage(perPage)
                .includeGrandTotal(true)
                .page(page, filter);
    }

    public int delete(String id) {
        final Optional<AuthzRoleDTO> role = mongoUtils.getById(id);
        final int delete = mongoUtils.deleteById(id) ? 1 : 0;
        if (delete > 0) {
            role.ifPresent(r -> userService.dissociateAllUsersFromRole(r.toLegacyRole()));
        }
        return delete;
    }

    private Bson buildRoleIdsQuery(SearchQuery searchQuery, Set<String> roleIds) {
        return Filters.and(MongoUtils.stringIdsIn(roleIds), searchQuery.toBson());
    }

    public Optional<AuthzRoleDTO> get(String roleId) {
        return mongoUtils.getById(roleId);
    }
}
