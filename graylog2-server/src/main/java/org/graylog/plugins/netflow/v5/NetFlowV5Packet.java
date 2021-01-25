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
package org.graylog.plugins.netflow.v5;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.util.List;

@AutoValue
public abstract class NetFlowV5Packet {
    public abstract NetFlowV5Header header();

    public abstract ImmutableList<NetFlowV5Record> records();

    public abstract long dataLength();

    public static NetFlowV5Packet create(NetFlowV5Header header, List<NetFlowV5Record> records, long dataLength) {
        return new AutoValue_NetFlowV5Packet(header, ImmutableList.copyOf(records), dataLength);
    }
}
