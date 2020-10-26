/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class GIMMapping7 extends GIMMapping {
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
}
