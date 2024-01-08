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

import org.graylog2.indexer.indexset.TemplateIndexSetConfig;
import org.graylog2.indexer.indices.Template;

/**
 * Implementing classes provide an index mapping template representation that can be stored in Elasticsearch.
 */
public interface IndexMappingTemplate {
    /**
     * Returns the index template as a map.
     *
     * @param indexSetConfig template-related index set configuration
     * @param order          the order value of the index template
     * @return the index template
     */
    Template toTemplate(TemplateIndexSetConfig indexSetConfig, Long order);

    /**
     * Returns the index template as a map. (with a default order of -1)
     *
     * @param indexSetConfig template-related index set configuration
     * @return the index template
     */
    default Template toTemplate(TemplateIndexSetConfig indexSetConfig) {
        return toTemplate(indexSetConfig, -1L);
    }
}
