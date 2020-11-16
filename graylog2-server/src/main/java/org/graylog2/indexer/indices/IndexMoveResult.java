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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
public abstract class IndexMoveResult {
    public abstract int movedDocuments();
    public abstract long tookMs();
    public abstract boolean hasFailedItems();

    public static IndexMoveResult create(int movedDocuments, long tookMs, boolean hasFailedItems) {
        return new AutoValue_IndexMoveResult(movedDocuments, tookMs, hasFailedItems);
    }
}
