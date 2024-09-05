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
package org.graylog2.rest.models.system.indices;

import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.rest.resources.system.indexer.responses.DataTieringStatus;

/**
 * Provides access to data-tiering features, but only when called in an environment that support it
 * (i.e. an Enterprise installation).
 * This allows callers to be agnostic, simplifying the code.
 */
public interface DataTieringStatusService {
    /**
     * Return data-tiering status, if available in the caller's environment
     */
    DataTieringStatus getStatus(IndexSet indexSet, IndexSetConfig indexSetConfig);
}
