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
package org.graylog.integrations.audit;

import com.google.common.collect.ImmutableSet;
import org.graylog2.audit.PluginAuditEventTypes;

import java.util.Set;

public class IntegrationsAuditEventTypes implements PluginAuditEventTypes {
    private static final String NAMESPACE = "integrations:";

    public static final String KINESIS_INPUT_CREATE = NAMESPACE + "kinesis_input:create";

    public static final String KINESIS_SETUP_CREATE_STREAM = NAMESPACE + "kinesis_auto_setup:create_stream";
    public static final String KINESIS_SETUP_CREATE_POLICY = NAMESPACE + "kinesis_auto_setup:create_policy";
    public static final String KINESIS_SETUP_CREATE_SUBSCRIPTION = NAMESPACE + "kinesis_auto_setup:create_subscription";


    private static final Set<String> EVENT_TYPES = ImmutableSet.<String>builder()
            .add(KINESIS_INPUT_CREATE)
            .add(KINESIS_SETUP_CREATE_STREAM)
            .add(KINESIS_SETUP_CREATE_POLICY)
            .add(KINESIS_SETUP_CREATE_SUBSCRIPTION)
            .build();

    @Override
    public Set<String> auditEventTypes() {
        return EVENT_TYPES;
    }
}
