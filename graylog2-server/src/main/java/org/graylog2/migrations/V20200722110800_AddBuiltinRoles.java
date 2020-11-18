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

import org.graylog2.plugin.security.PluginPermissions;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.Set;

public class V20200722110800_AddBuiltinRoles extends Migration {
    private final MigrationHelpers helpers;
    private final Set<PluginPermissions> pluginPermissions;

    @Inject
    public V20200722110800_AddBuiltinRoles(MigrationHelpers helpers,
                                           Set<PluginPermissions> pluginPermissions) {
        this.helpers = helpers;
        this.pluginPermissions = pluginPermissions;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2020-07-22T11:08:00Z");
    }

    @Override
    public void upgrade() {
        for (PluginPermissions permission: pluginPermissions) {
            permission.builtinRoles().forEach(r -> helpers.ensureBuiltinRole(r.name(), r.description(), r.permissions()));
        }
    }
}
