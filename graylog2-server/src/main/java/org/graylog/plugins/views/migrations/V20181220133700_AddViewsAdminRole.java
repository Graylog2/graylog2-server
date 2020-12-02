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
package org.graylog.plugins.views.migrations;

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.rest.ViewsRestPermissions;
import org.graylog2.migrations.Migration;
import org.graylog2.migrations.MigrationHelpers;

import javax.inject.Inject;
import java.time.ZonedDateTime;

public class V20181220133700_AddViewsAdminRole extends Migration {
    private final MigrationHelpers helpers;

    @Inject
    public V20181220133700_AddViewsAdminRole(MigrationHelpers helpers) {
        this.helpers = helpers;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2018-12-20T13:37:00Z");
    }

    @Override
    public void upgrade() {
        helpers.ensureBuiltinRole("Views Manager", "Allows reading and writing all views and extended searches (built-in)", ImmutableSet.of(
                ViewsRestPermissions.VIEW_READ,
                ViewsRestPermissions.VIEW_EDIT
        ));
    }
}
