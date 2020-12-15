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
package org.graylog.events.audit;

import com.google.common.collect.ImmutableSet;
import org.graylog2.audit.PluginAuditEventTypes;

import java.util.Set;

public class EventsAuditEventTypes implements PluginAuditEventTypes {
    private static final String EVENT_DEFINITON_PREFIX = "events:definition:";
    private static final String EVENT_NOTIFICATION_PREFIX = "events:notification:";

    public static final String EVENT_DEFINITION_CREATE = EVENT_DEFINITON_PREFIX + "create";
    public static final String EVENT_DEFINITION_DELETE = EVENT_DEFINITON_PREFIX + "delete";
    public static final String EVENT_DEFINITION_EXECUTE = EVENT_DEFINITON_PREFIX + "execute";
    public static final String EVENT_DEFINITION_UPDATE = EVENT_DEFINITON_PREFIX + "update";
    public static final String EVENT_NOTIFICATION_CREATE = EVENT_NOTIFICATION_PREFIX + "create";
    public static final String EVENT_NOTIFICATION_DELETE = EVENT_NOTIFICATION_PREFIX + "delete";
    public static final String EVENT_NOTIFICATION_UPDATE = EVENT_NOTIFICATION_PREFIX + "update";

    private static final ImmutableSet<String> EVENT_TYPES = ImmutableSet.<String>builder()
        .add(EVENT_DEFINITION_CREATE)
        .add(EVENT_DEFINITION_DELETE)
        .add(EVENT_DEFINITION_EXECUTE)
        .add(EVENT_DEFINITION_UPDATE)
        .add(EVENT_NOTIFICATION_CREATE)
        .add(EVENT_NOTIFICATION_DELETE)
        .add(EVENT_NOTIFICATION_UPDATE)
        .build();

    @Override
    public Set<String> auditEventTypes() {
        return EVENT_TYPES;
    }
}
