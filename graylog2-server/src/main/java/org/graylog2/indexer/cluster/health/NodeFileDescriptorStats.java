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
import java.util.Optional;

@AutoValue
public abstract class NodeFileDescriptorStats {
    public abstract String name();

    public abstract String ip();

    @Nullable
    public abstract String host();

    public abstract Optional<Long> fileDescriptorMax();

    public static NodeFileDescriptorStats create(String name, String ip, @Nullable String host, Long fileDescriptorMax) {
        return new AutoValue_NodeFileDescriptorStats(name, ip, host, Optional.ofNullable(fileDescriptorMax));
    }
}
