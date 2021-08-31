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
package org.graylog2.indexer;

import com.github.zafarkhaja.semver.Version;

/**
 * A factory creating an index mapping template for the failure index.
 * Since ES failure storage is not a part of Graylog Open, an actual
 * implementation of this interface must be provided in a plugin.
 */
public interface FailureIndexMappingFactory {

    /**
     * Creates an index mapping template for the failure index
     * @param elasticsearchVersion a target version of Elastic Search
     */
    IndexMappingTemplate failureIndexMappingFor(Version elasticsearchVersion);
}
