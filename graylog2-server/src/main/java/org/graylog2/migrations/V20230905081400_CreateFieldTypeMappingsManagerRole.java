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
package org.graylog2.migrations;

import org.graylog2.shared.security.RestPermissions;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.Set;

public class V20230905081400_CreateFieldTypeMappingsManagerRole extends Migration {
    private final MigrationHelpers helpers;

    @Inject
    public V20230905081400_CreateFieldTypeMappingsManagerRole(MigrationHelpers migrationHelpers) {
        this.helpers = migrationHelpers;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2023-09-05T08:14:00Z");
    }

    @Override
    public void upgrade() {
        helpers.ensureBuiltinRole("Field Type Mappings Manager", "Grants full control over custom field type mappings for all index sets (built-in)",
                Set.of(RestPermissions.TYPE_MAPPINGS_CREATE, RestPermissions.TYPE_MAPPINGS_DELETE, RestPermissions.TYPE_MAPPINGS_EDIT, RestPermissions.TYPE_MAPPINGS_READ));
    }
}
