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

import org.graylog.security.HasPermissions;

import java.util.List;
import java.util.function.BiFunction;

import static org.graylog2.plugin.quickjump.QuickJumpConstants.DEFAULT_FIELDS;

public interface QuickJumpProvider {
    String collectionName();

    boolean isPermitted(String id, HasPermissions user);

    List<String> fieldsToSearch();

    static QuickJumpProvider create(String collectionName, BiFunction<String, HasPermissions, Boolean> isPermittedFn, List<String> fieldsToSearch) {
        return new QuickJumpProvider() {
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
        };
    }

    static QuickJumpProvider create(String collectionName, BiFunction<String, HasPermissions, Boolean> isPermittedFn) {
        return create(collectionName, isPermittedFn, DEFAULT_FIELDS);
    }
}
