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
package org.graylog.plugins.pipelineprocessor.audit;

import com.google.common.collect.ImmutableSet;
import org.graylog2.audit.PluginAuditEventTypes;

import java.util.Set;

public class PipelineProcessorAuditEventTypes implements PluginAuditEventTypes {
    private static final String NAMESPACE = "pipeline_processor:";

    public static final String PIPELINE_CONNECTION_UPDATE = NAMESPACE + "pipeline_connection:update";
    public static final String PIPELINE_CREATE = NAMESPACE + "pipeline:create";
    public static final String PIPELINE_UPDATE = NAMESPACE + "pipeline:update";
    public static final String PIPELINE_DELETE = NAMESPACE + "pipeline:delete";
    public static final String RULE_CREATE = NAMESPACE + "rule:create";
    public static final String RULE_UPDATE = NAMESPACE + "rule:update";
    public static final String RULE_DELETE = NAMESPACE + "rule:delete";
    public static final String RULE_METRICS_UPDATE = NAMESPACE + "rulemetrics:update";

    private static final ImmutableSet<String> EVENT_TYPES = ImmutableSet.<String>builder()
            .add(PIPELINE_CONNECTION_UPDATE)
            .add(PIPELINE_CREATE)
            .add(PIPELINE_UPDATE)
            .add(PIPELINE_DELETE)
            .add(RULE_CREATE)
            .add(RULE_UPDATE)
            .add(RULE_DELETE)
            .add(RULE_METRICS_UPDATE)
            .build();

    @Override
    public Set<String> auditEventTypes() {
        return EVENT_TYPES;
    }
}
