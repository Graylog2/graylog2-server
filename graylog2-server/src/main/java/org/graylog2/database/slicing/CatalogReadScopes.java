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
package org.graylog2.database.slicing;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import jakarta.inject.Inject;
import org.apache.shiro.subject.Subject;
import org.bson.Document;
import org.graylog2.database.DbEntity;
import org.graylog2.database.MongoConnection;
import org.graylog2.shared.security.EntityPermissionsUtils;

import java.util.List;
import java.util.function.Predicate;

import static org.graylog2.database.utils.MongoUtils.stream;
import static org.graylog2.shared.security.EntityPermissionsUtils.ID_FIELD;
import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Builds the {@link ReadScope} of a collection registered in the
 * {@link org.graylog2.database.dbcatalog.DbEntitiesCatalog DbEntitiesCatalog}, from its read permission, e.g.
 * {@code streams:read}. A user may read the entities whose id their permissions grant it for, like
 * {@code streams:read:<id>}.
 * <p>
 * Users who may read the whole collection get an unrestricted scope at no cost. For everyone else, the scope lists
 * the ids of the entities they may read, which requires checking every entity of the collection. That suits
 * configuration-sized collections like streams or dashboards. Collections whose read access follows from a field of
 * their documents, like events and their streams, should build their scope from that field instead.
 */
public class CatalogReadScopes {
    private final MongoConnection mongoConnection;
    private final EntityPermissionsUtils permissionsUtils;

    @Inject
    public CatalogReadScopes(final MongoConnection mongoConnection, final EntityPermissionsUtils permissionsUtils) {
        this.mongoConnection = mongoConnection;
        this.permissionsUtils = permissionsUtils;
    }

    /**
     * @throws IllegalArgumentException if the collection isn't registered in the catalog, so its read permission is
     *                                  unknown
     */
    public ReadScope readScope(final String collection, final Subject subject) {
        final String readPermission = permissionsUtils.readPermissionForCollection(collection)
                .orElseThrow(() -> new IllegalArgumentException(
                        f("Collection <%s> isn't registered in the entities catalog", collection)));
        if (permissionsUtils.hasAllPermission(subject)
                || permissionsUtils.hasReadPermissionForWholeCollection(subject, collection)) {
            return ReadScope.unrestricted();
        }
        if (DbEntity.NOBODY_ALLOWED.equals(readPermission)) {
            return ReadScope.of(Filters.in(ID_FIELD, List.of()));
        }

        final Predicate<Document> isReadable = permissionsUtils.createPermissionCheck(subject, collection);
        final var ids = mongoConnection.getMongoDatabase().getCollection(collection)
                .find()
                .projection(Projections.include(ID_FIELD));
        try (final var documents = stream(ids)) {
            return ReadScope.of(Filters.in(ID_FIELD, documents.filter(isReadable)
                    .map(document -> document.get(ID_FIELD))
                    .toList()));
        }
    }
}
