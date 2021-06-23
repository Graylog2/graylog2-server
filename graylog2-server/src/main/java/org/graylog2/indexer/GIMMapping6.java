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

public class GIMMapping6 extends GIMMapping {
    @Override
    Map<String, Object> dynamicStrings() {
        return ImmutableMap.of(
                "match_mapping_type", "string",
                "mapping", notAnalyzedString()
        );
    }

    @Override
    protected String dateFormats() {
        return "8yyyy-MM-dd HH:mm:ss||8yyyy-MM-dd";
    }
}
