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
package org.graylog.scheduler.audit;

import com.google.common.collect.ImmutableSet;
import org.graylog2.audit.PluginAuditEventTypes;

import java.util.Set;

public class JobSchedulerAuditEventTypes implements PluginAuditEventTypes {
    public static final String SCHEDULER_JOB_CREATE = "scheduler:job:create";
    public static final String SCHEDULER_JOB_DELETE = "scheduler:job:delete";
    public static final String SCHEDULER_JOB_UPDATE = "scheduler:job:update";
    public static final String SCHEDULER_TRIGGER_CREATE = "scheduler:trigger:create";

    private static final ImmutableSet<String> EVENT_TYPES = ImmutableSet.<String>builder()
        .add(SCHEDULER_JOB_CREATE)
        .add(SCHEDULER_JOB_DELETE)
        .add(SCHEDULER_JOB_UPDATE)
        .add(SCHEDULER_TRIGGER_CREATE)
        .build();

    @Override
    public Set<String> auditEventTypes() {
        return EVENT_TYPES;
    }
}
