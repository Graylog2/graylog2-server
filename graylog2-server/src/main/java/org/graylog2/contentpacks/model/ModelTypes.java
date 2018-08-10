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
package org.graylog2.contentpacks.model;

public interface ModelTypes {
    ModelType COLLECTOR_CONFIGURATION = ModelType.of("collector_configuration", "1");
    ModelType COLLECTOR = ModelType.of("collector", "1");
    ModelType DASHBOARD = ModelType.of("dashboard", "1");
    ModelType GROK_PATTERN = ModelType.of("grok_pattern", "1");
    ModelType LOOKUP_ADAPTER = ModelType.of("lookup_adapter", "1");
    ModelType LOOKUP_CACHE = ModelType.of("lookup_cache", "1");
    ModelType LOOKUP_TABLE = ModelType.of("lookup_table", "1");
    ModelType INPUT = ModelType.of("input", "1");
    ModelType OUTPUT = ModelType.of("output", "1");
    ModelType PIPELINE = ModelType.of("pipeline", "1");
    ModelType PIPELINE_RULE = ModelType.of("pipeline_rule", "1");
    ModelType ROOT = ModelType.of("virtual-root", "1");
    ModelType STREAM = ModelType.of("stream", "1");
}
