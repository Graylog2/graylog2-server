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
package org.graylog2.rest.models.system.inputs.extractors.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import java.util.List;
import java.util.Map;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class ExtractorSummary {
    @JsonProperty
    public abstract String id();

    @JsonProperty
    public abstract String title();

    @JsonProperty
    public abstract String type();

    @JsonProperty("cursor_strategy")
    public abstract String cursorStrategy();

    @JsonProperty("source_field")
    public abstract String sourceField();

    @JsonProperty("target_field")
    public abstract String targetField();

    @JsonProperty("extractor_config")
    public abstract Map<String, Object> extractorConfig();

    @JsonProperty("creator_user_id")
    public abstract String creatorUserId();

    @JsonProperty
    public abstract List<Map<String, Object>> converters();

    @JsonProperty("condition_type")
    public abstract String conditionType();

    @JsonProperty("condition_value")
    public abstract String conditionValue();

    @JsonProperty
    public abstract Long order();

    @JsonProperty
    public abstract Long exceptions();

    @JsonProperty("converter_exceptions")
    public abstract Long converterExceptions();

    @JsonProperty
    public abstract ExtractorMetrics metrics();

    @JsonCreator
    public static ExtractorSummary create(@JsonProperty("id") String id,
                                          @JsonProperty("title") String title,
                                          @JsonProperty("type") String type,
                                          @JsonProperty("cursor_strategy") String cursorStrategy,
                                          @JsonProperty("source_field") String sourceField,
                                          @JsonProperty("target_field") String targetField,
                                          @JsonProperty("extractor_config") Map<String, Object> extractorConfig,
                                          @JsonProperty("creator_user_id") String creatorUserId,
                                          @JsonProperty("converters") List<Map<String, Object>> converters,
                                          @JsonProperty("condition_type") String conditionType,
                                          @JsonProperty("condition_value") String conditionValue,
                                          @JsonProperty("order") Long order,
                                          @JsonProperty("exceptions") Long exceptions,
                                          @JsonProperty("converter_exceptions") Long converterExceptions,
                                          @JsonProperty("metrics") ExtractorMetrics metrics) {
        return new AutoValue_ExtractorSummary(id, title, type, cursorStrategy, sourceField, targetField, extractorConfig, creatorUserId, converters, conditionType, conditionValue, order, exceptions, converterExceptions, metrics);
    }
}
