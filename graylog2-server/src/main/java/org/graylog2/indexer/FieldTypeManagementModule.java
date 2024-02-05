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
package org.graylog2.indexer;

import org.graylog2.migrations.V20230905081400_CreateFieldTypeMappingsManagerRole;
import org.graylog2.migrations.V20240118130000_FieldTypeMappingsManagerRoleReceivesProfilePermissions;
import org.graylog2.plugin.PluginModule;
import org.graylog2.rest.resources.system.field_types.FieldTypeMappingsResource;

public class FieldTypeManagementModule extends PluginModule {
    @Override
    protected void configure() {
        addSystemRestResource(FieldTypeMappingsResource.class);
        addMigration(V20230905081400_CreateFieldTypeMappingsManagerRole.class);
        addMigration(V20240118130000_FieldTypeMappingsManagerRoleReceivesProfilePermissions.class);
    }
}
