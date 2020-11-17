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
import com.mongodb.client.model.Projections;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.graylog2.search.SearchQuery;
import org.graylog2.shared.users.UserService;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

public class PaginatedAuthzRolesService extends PaginatedDbService<AuthzRoleDTO> {
    private static final String COLLECTION_NAME = "roles";

    private final MongoCollection<Document> dbCollection;
    private final UserService userService;

    @Inject
    public PaginatedAuthzRolesService(MongoConnection mongoConnection,
                                      UserService userService,
                                      MongoJackObjectMapperProvider mapper) {
        super(mongoConnection, mapper, AuthzRoleDTO.class, COLLECTION_NAME);
        this.dbCollection = mongoConnection.getMongoDatabase().getCollection(COLLECTION_NAME);
        this.userService = userService;
    }

    public long count() {
        return dbCollection.countDocuments();
    }

    public ImmutableSet<String> getAllRoleIds() {
        // Use a MongoCollection query here to avoid the mongojack deserializing and object creation overhead
        final FindIterable<Document> docs = dbCollection.find().projection(Projections.include("_id"));

        return StreamSupport.stream(docs.spliterator(), false)
                .map(doc -> doc.get("_id", ObjectId.class).toHexString())
                .collect(ImmutableSet.toImmutableSet());
    }

    public List<AuthzRoleDTO> findByIds(Collection<String> ids) {
        return asImmutableList(db.find(DBQuery.in("_id", ids)));
    }

    public PaginatedList<AuthzRoleDTO> findPaginated(SearchQuery searchQuery, int page,
                                                     int perPage, String sortField, String order) {
        final DBQuery.Query dbQuery = searchQuery.toDBQuery();
        final DBSort.SortBuilder sortBuilder = getSortBuilder(order, sortField);
        return findPaginatedWithQueryAndSort(dbQuery, sortBuilder, page, perPage);
    }

    public PaginatedList<AuthzRoleDTO> findPaginatedByIds(SearchQuery searchQuery,
                                                          int page,
                                                          int perPage,
                                                          String sortField,
                                                          String order,
                                                          Set<String> roleIds) {
        final DBQuery.Query dbQuery = buildRoleIdsQuery(searchQuery, roleIds);
        final DBSort.SortBuilder sortBuilder = getSortBuilder(order, sortField);

        return findPaginatedWithQueryAndSort(dbQuery, sortBuilder, page, perPage);
    }

    public PaginatedList<AuthzRoleDTO> findPaginatedByIdsWithFilter(SearchQuery searchQuery,
                                                                    Predicate<AuthzRoleDTO> filter,
                                                                    int page,
                                                                    int perPage,
                                                                    String sortField,
                                                                    String order,
                                                                    Set<String> roleIds) {
        final DBQuery.Query dbQuery = buildRoleIdsQuery(searchQuery, roleIds);
        final DBSort.SortBuilder sortBuilder = getSortBuilder(order, sortField);

        return findPaginatedWithQueryFilterAndSort(dbQuery, filter, sortBuilder, page, perPage);
    }

    @Override
    public int delete(String id) {
        final Optional<AuthzRoleDTO> role = get(id);
        final int delete = super.delete(id);
        if (delete > 0) {
            role.ifPresent(r -> userService.dissociateAllUsersFromRole(r.toLegacyRole()));
        }
        return delete;
    }

    private DBQuery.Query buildRoleIdsQuery(SearchQuery searchQuery, Set<String> roleIds) {
        return DBQuery.and(DBQuery.in("_id", roleIds), searchQuery.toDBQuery());
    }
}
