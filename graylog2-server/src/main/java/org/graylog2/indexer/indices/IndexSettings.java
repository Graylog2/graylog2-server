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
package org.graylog2.indexer.indices;

import com.google.common.collect.ImmutableMap;
import jakarta.annotation.Nullable;

import java.util.Map;

public class IndexSettings {
    private final Map<String, Object> settings;

    public IndexSettings(Map<String, Object> settings) {
        this.settings = settings;
    }

    public static IndexSettings create(int shards, int replicas, @Nullable Map<String, Object> analysis) {
        ImmutableMap.Builder<String, Object> fields = ImmutableMap.<String, Object>builder()
                .put("number_of_shards", shards)
                .put("number_of_replicas", replicas);
        if (analysis != null) {
            fields.put("analysis", analysis);
        }
        return new IndexSettings(fields.build());
    }

    public Map<String, Object> map() {
        return settings;
    }
}
