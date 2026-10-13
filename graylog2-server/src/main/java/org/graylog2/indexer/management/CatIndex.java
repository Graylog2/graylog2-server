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
 * One index as the backend lists it. Closed indices have no health, docs or size.
 */
public record CatIndex(String index,
                       @Nullable String health,
                       @Nullable String status,
                       @Nullable Integer primaryShards,
                       @Nullable Integer replicas,
                       @Nullable Long docsCount,
                       @Nullable Long storeSizeBytes) {

    /**
     * From the backend's columns, which are all strings: counts and the size in bytes may be missing or blank.
     */
    public static CatIndex of(String index, @Nullable String health, @Nullable String status, @Nullable String primaryShards,
                              @Nullable String replicas, @Nullable String docsCount, @Nullable String storeSizeBytes) {
        return new CatIndex(index, health, status, integer(primaryShards), integer(replicas), number(docsCount),
                number(storeSizeBytes));
    }

    private static Long number(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer integer(@Nullable String value) {
        final Long number = number(value);
        return number == null ? null : number.intValue();
    }
}
