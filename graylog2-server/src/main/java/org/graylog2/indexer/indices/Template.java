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
package org.graylog2.indexer.indices;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record Template(List<String> indexPatterns, Mappings mappings, Long order, Settings settings) {
    public static class Mappings extends HashMap<String, Object> {
        public Mappings(Map<String, Object> m) {
            super(m);
        }
    }

    public static class Settings extends HashMap<String, Object> {
        public Settings(Map<String, Object> m) {
            super(m);
        }
    }

    public static Template create(List<String> indexPatterns, Mappings mappings, Long order, Settings settings) {
        return new Template(indexPatterns, mappings, order, settings);
    }

    public static Template create(String indexPattern, Mappings mappings, Long order, Settings settings) {
        return Template.create(Collections.singletonList(indexPattern), mappings, order, settings);
    }
}
