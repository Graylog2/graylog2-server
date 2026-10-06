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
package org.graylog2.indexer.management;

import javax.annotation.Nullable;

/**
 * One shard copy. {@code node} is the node it is on (for a relocating copy, the node it is moving from);
 * the {@code unassigned*} fields are set only for an unassigned copy.
 */
public record CatShard(String index,
                       int shard,
                       boolean primary,
                       String state,
                       @Nullable String node,
                       @Nullable String unassignedReason,
                       @Nullable String unassignedAt,
                       @Nullable String unassignedDetails) {
    public static final String STATE_UNASSIGNED = "UNASSIGNED";

    public boolean isUnassigned() {
        return STATE_UNASSIGNED.equals(state);
    }
}
