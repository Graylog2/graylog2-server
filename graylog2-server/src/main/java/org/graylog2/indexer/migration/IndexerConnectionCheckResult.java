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
package org.graylog2.indexer.migration;

import java.util.Collections;
import java.util.List;

public record IndexerConnectionCheckResult(List<RemoteIndex> indices, String error) {

    public static IndexerConnectionCheckResult success(List<RemoteIndex> indexNames) {
        return new IndexerConnectionCheckResult(indexNames, null);
    }

    public static IndexerConnectionCheckResult failure(Exception e) {
        return new IndexerConnectionCheckResult(Collections.emptyList(), e.getMessage());
    }

    public static IndexerConnectionCheckResult failure(String errorMessage) {
        return new IndexerConnectionCheckResult(Collections.emptyList(), errorMessage);
    }
}
