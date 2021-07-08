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
package org.graylog.failure;

import org.graylog2.indexer.IndexFailure;

public class IndexingFailure implements Failure{

    private final IndexFailure internalFailure;

    public IndexingFailure(IndexFailure internalFailure) {
        this.internalFailure = internalFailure;
    }

    public IndexFailure getInternalFailure() {
        return internalFailure;
    }

    @Override
    public String type() {
        return "indexing";
    }

    @Override
    public String toString() {
        return internalFailure.asMap().toString();
    }
}
