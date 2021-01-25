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

import org.graylog2.indexer.indexset.IndexSetConfig;

import java.util.Map;

/**
 * Implementing classes provide an index mapping template representation that can be stored in Elasticsearch.
 */
public interface IndexMappingTemplate {
    /**
     * Returns the index template as a map.
     *
     * @param indexSetConfig the index set configuration
     * @param indexPattern   the index pattern the returned template should be applied to
     * @param order          the order value of the index template
     * @return the index template
     */
    Map<String, Object> toTemplate(IndexSetConfig indexSetConfig, String indexPattern, int order);

    /**
     * Returns the index template as a map. (with an default order of -1)
     *
     * @param indexSetConfig the index set configuration
     * @param indexPattern   the index pattern the returned template should be applied to
     * @return the index template
     */
    default Map<String, Object> toTemplate(IndexSetConfig indexSetConfig, String indexPattern) {
        return toTemplate(indexSetConfig, indexPattern, -1);
    }
}
