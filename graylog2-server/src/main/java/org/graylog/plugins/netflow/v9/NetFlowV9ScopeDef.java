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

@AutoValue
public abstract class NetFlowV9ScopeDef {
    public static final int SYSTEM = 1;
    public static final int INTERFACE = 2;
    public static final int LINECARD = 3;
    public static final int NETFLOW_CACHE = 4;
    public static final int TEMPLATE = 5;

    public abstract int type();

    public abstract int length();

    public static NetFlowV9ScopeDef create(int type, int length) {
        return new AutoValue_NetFlowV9ScopeDef(type, length);
    }
}
