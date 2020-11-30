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
package org.graylog2.indexer.cluster.health;

import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

@AutoValue
public abstract class NodeDiskUsageStats {
    public static final double DEFAULT_DISK_USED_PERCENT = -1D;

    public abstract String name();

    public abstract String ip();

    @Nullable
    public abstract String host();

    public abstract ByteSize diskTotal();

    public abstract ByteSize diskUsed();

    public abstract ByteSize diskAvailable();

    public abstract Double diskUsedPercent();

    public static NodeDiskUsageStats create(String name, String ip, @Nullable String host, String diskUsedString, String diskTotalString, Double diskUsedPercent) {
        ByteSize diskTotal = SIUnitParser.parseBytesSizeValue(diskTotalString);
        ByteSize diskUsed = SIUnitParser.parseBytesSizeValue(diskUsedString);
        ByteSize diskAvailable = () -> diskTotal.getBytes() - diskUsed.getBytes();
        return new AutoValue_NodeDiskUsageStats(
                name,
                ip,
                host,
                diskTotal,
                diskUsed,
                diskAvailable,
                diskUsedPercent
        );
    }
}
