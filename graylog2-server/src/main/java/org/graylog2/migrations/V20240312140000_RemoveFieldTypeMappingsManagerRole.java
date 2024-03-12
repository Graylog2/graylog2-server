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

import jakarta.inject.Inject;

import java.time.ZonedDateTime;

public class V20240312140000_RemoveFieldTypeMappingsManagerRole extends Migration {

    private final RoleRemover helpers;

    @Inject
    public V20240312140000_RemoveFieldTypeMappingsManagerRole(final RoleRemover roleRemover) {
        this.helpers = roleRemover;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2024-03-12T14:00:00Z");
    }

    @Override
    public void upgrade() {
        helpers.removeBuiltinRole("Field Type Mappings Manager");
    }
}
