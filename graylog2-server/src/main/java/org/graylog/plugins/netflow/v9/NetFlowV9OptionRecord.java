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
package org.graylog.plugins.netflow.v9;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

@AutoValue
public abstract class NetFlowV9OptionRecord implements NetFlowV9BaseRecord {
    @Override
    public abstract ImmutableMap<String, Object> fields();

    public abstract ImmutableMap<Integer, Object> scopes();

    public static NetFlowV9OptionRecord create(Map<String, Object> fields, Map<Integer, Object> scopes) {
        return new AutoValue_NetFlowV9OptionRecord(ImmutableMap.copyOf(fields), ImmutableMap.copyOf(scopes));
    }
}