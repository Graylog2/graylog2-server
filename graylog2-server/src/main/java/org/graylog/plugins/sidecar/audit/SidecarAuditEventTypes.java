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
package org.graylog.plugins.sidecar.audit;

import com.google.common.collect.ImmutableSet;
import org.graylog2.audit.PluginAuditEventTypes;

import java.util.Set;

public class SidecarAuditEventTypes implements PluginAuditEventTypes {
    private static final String NAMESPACE = "sidecar:";

    public static final String ACTION_UPDATE = NAMESPACE + "action:update";

    public static final String SIDECAR_UPDATE = NAMESPACE + "sidecar:update";
    public static final String SIDECAR_DELETE = NAMESPACE + "sidecar:delete";

    public static final String COLLECTOR_CREATE = NAMESPACE + "collector:create";
    public static final String COLLECTOR_UPDATE = NAMESPACE + "collector:update";
    public static final String COLLECTOR_DELETE = NAMESPACE + "collector:delete";
    public static final String COLLECTOR_CLONE = NAMESPACE + "collector:clone";

    public static final String CONFIGURATION_CREATE = NAMESPACE + "configuration:create";
    public static final String CONFIGURATION_UPDATE = NAMESPACE + "configuration:update";
    public static final String CONFIGURATION_DELETE = NAMESPACE + "configuration:delete";
    public static final String CONFIGURATION_CLONE = NAMESPACE + "configuration:clone";

    public static final String CONFIGURATION_VARIABLE_CREATE = NAMESPACE + "configuration_variable:create";
    public static final String CONFIGURATION_VARIABLE_UPDATE = NAMESPACE + "configuration_variable:update";
    public static final String CONFIGURATION_VARIABLE_DELETE = NAMESPACE + "configuration_variable:delete";

    private static final Set<String> EVENT_TYPES = ImmutableSet.<String>builder()
            .add(ACTION_UPDATE)
            .add(SIDECAR_UPDATE)
            .add(SIDECAR_DELETE)
            .add(COLLECTOR_CREATE)
            .add(COLLECTOR_UPDATE)
            .add(COLLECTOR_DELETE)
            .add(COLLECTOR_CLONE)
            .add(CONFIGURATION_CREATE)
            .add(CONFIGURATION_UPDATE)
            .add(CONFIGURATION_DELETE)
            .add(CONFIGURATION_CLONE)
            .add(CONFIGURATION_VARIABLE_CREATE)
            .add(CONFIGURATION_VARIABLE_UPDATE)
            .add(CONFIGURATION_VARIABLE_DELETE)
            .build();

    @Override
    public Set<String> auditEventTypes() {
        return EVENT_TYPES;
    }
}
