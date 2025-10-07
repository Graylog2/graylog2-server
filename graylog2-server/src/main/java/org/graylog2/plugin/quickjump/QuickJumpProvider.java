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
package org.graylog2.plugin.quickjump;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog.security.HasPermissions;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import static com.google.common.base.Preconditions.checkArgument;
import static org.graylog2.plugin.quickjump.QuickJumpConstants.DEFAULT_FIELDS;

public interface QuickJumpProvider {
    String type();

    String collectionName();

    boolean isPermitted(String id, HasPermissions user);

    List<String> fieldsToSearch();

    default Bson typeField() {
        return new Document("$literal", type());
    }

    static QuickJumpProvider create(String type, String collectionName, BiFunction<String, HasPermissions, Boolean> isPermittedFn, List<String> fieldsToSearch, Optional<Bson> typeField) {
        checkArgument(fieldsToSearch != null && !fieldsToSearch.isEmpty(), "fieldsToSearch must not be null or empty");
        return new QuickJumpProvider() {

            @Override
            public String type() {
                return type;
            }

            @Override
            public String collectionName() {
                return collectionName;
            }

            @Override
            public boolean isPermitted(String id, HasPermissions user) {
                return isPermittedFn.apply(id, user);
            }

            @Override
            public List<String> fieldsToSearch() {
                return fieldsToSearch;
            }

            @Override
            public Bson typeField() {
                return typeField.orElseGet(QuickJumpProvider.super::typeField);
            }
        };
    }

    static QuickJumpProvider create(String type, String collectionName, BiFunction<String, HasPermissions, Boolean> isPermittedFn, List<String> fieldsToSearch) {
        return create(type, collectionName, isPermittedFn, fieldsToSearch, Optional.empty());
    }

    static QuickJumpProvider create(String type, String collectionName, BiFunction<String, HasPermissions, Boolean> isPermittedFn) {
        return create(type, collectionName, isPermittedFn, DEFAULT_FIELDS, Optional.empty());
    }

    static QuickJumpProvider create(String type, String collectionName, BiFunction<String, HasPermissions, Boolean> isPermittedFn, Bson typeField) {
        return create(type, collectionName, isPermittedFn, DEFAULT_FIELDS, Optional.of(typeField));
    }
}
