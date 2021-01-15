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

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Representing the message type mapping in Elasticsearch 5.x. This is giving ES more
 * information about what the fields look like and how it should analyze them.
 *
 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/5.4/mapping.html">Elasticsearch Reference / Mapping</a>
 */
class IndexMapping5 extends IndexMapping {
    @Override
    Map<String, Object> dynamicStrings() {
        return ImmutableMap.of(
                // Match all
                "match", "*",
                // Analyze nothing by default
                "mapping", ImmutableMap.of("index", "not_analyzed"));
    }
}
