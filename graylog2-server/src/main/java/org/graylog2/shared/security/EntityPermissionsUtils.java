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
package org.graylog2.shared.security;

import jakarta.inject.Inject;
import org.apache.shiro.authz.permission.AllPermission;
import org.apache.shiro.subject.Subject;
import org.bson.Document;
import org.graylog2.database.DbEntity;
import org.graylog2.database.dbcatalog.DbEntitiesCatalog;
import org.graylog2.database.dbcatalog.DbEntityCatalogEntry;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class EntityPermissionsUtils {

    public static final String ID_FIELD = "_id";

    private final DbEntitiesCatalog catalog;

    @Inject
    public EntityPermissionsUtils(final DbEntitiesCatalog catalog) {
        this.catalog = catalog;
    }

    public Predicate<Document> createPermissionCheck(final Subject subject, final String collection) {
        final var readPermission = readPermissionForCollection(collection);
        return doc -> readPermission
                .map(permission -> subject.isPermitted(permission + ":" + doc.getObjectId(ID_FIELD).toString()))
                .orElse(false);
    }

    public boolean hasAllPermission(final Subject subject) {
        return subject.isPermitted(new AllPermission());
    }

    public boolean hasReadPermissionForWholeCollection(final Subject subject,
                                                       final String collection) {
        return readPermissionForCollection(collection)
                .map(rp -> rp.equals(DbEntity.ALL_ALLOWED) || subject.isPermitted(rp + ":*"))
                .orElse(false);
    }

    public Optional<String> readPermissionForCollection(final String collection) {
        return catalog.getByCollectionName(collection)
                .map(DbEntityCatalogEntry::readPermission);
    }

    /**
     * Checks whether all the given field names are declared as readable for the specified collection.
     * The {@code _id} field is always considered readable.
     * If the collection is not registered in the catalog, all fields are considered readable (backward compatibility).
     * If the catalog entry has an empty {@code readableFields} list, no user-specified fields are allowed.
     */
    public boolean areFieldsReadable(final String collection, final Collection<String> fields) {
        final Optional<DbEntityCatalogEntry> entry = catalog.getByCollectionName(collection);
        if (entry.isEmpty()) {
            return true;
        }
        final List<String> readableFields = entry.get().readableFields();
        if (readableFields.isEmpty()) {
            return fields.stream().allMatch(ID_FIELD::equals);
        }
        return fields.stream().allMatch(f -> ID_FIELD.equals(f) || readableFields.contains(f));
    }
}
