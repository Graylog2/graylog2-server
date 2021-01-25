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

public class IndexMapping7 extends IndexMapping {
    @Override
    protected Map<String, Object> mapping(String analyzer) {
        return messageMapping(analyzer);
    }

    @Override
    Map<String, Object> dynamicStrings() {
        return ImmutableMap.of(
                "match_mapping_type", "string",
                "mapping", notAnalyzedString()
        );
    }

    @Override
    Map<String, Object> createTemplate(String template, int order, Map<String, Object> settings, Map<String, Object> mappings) {
        return ImmutableMap.of(
                "index_patterns", template,
                "order", order,
                "settings", settings,
                "mappings", mappings
        );
    }

    @Override
    protected String dateFormat() {
        return ConstantsES7.ES_DATE_FORMAT;
    }
}
