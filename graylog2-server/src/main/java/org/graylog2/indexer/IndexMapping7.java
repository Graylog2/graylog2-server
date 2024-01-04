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
import org.graylog2.indexer.indexset.CustomFieldMappings;
import org.graylog2.indexer.indices.Template;

import java.util.Map;

public class IndexMapping7 extends IndexMapping {
    @Override
    protected Template.Mappings mapping(String analyzer,
                                          final CustomFieldMappings customFieldMappings) {
        return new Template.Mappings(messageMapping(analyzer, customFieldMappings));
    }

    @Override
    Map<String, Object> dynamicStrings() {
        return ImmutableMap.of(
                "match_mapping_type", "string",
                "mapping", notAnalyzedString()
        );
    }

    @Override
    protected String dateFormat() {
        return ConstantsES7.ES_DATE_FORMAT;
    }
}
