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
package org.graylog2.indexer.indexset;

import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Collects the fields to keep mapped when dynamic mapping is turned off on an index set.
 */
public interface KeptFieldsCollector {

    /**
     * @return the fields to keep, or null if the index set's template needs none
     */
    @Nullable
    List<KeptField> collect(IndexSetConfig config);
}
