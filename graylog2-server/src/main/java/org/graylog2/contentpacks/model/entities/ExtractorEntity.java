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
/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY(); without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.contentpacks.model.entities;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.inputs.Extractor;

import java.util.List;
import java.util.Map;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class ExtractorEntity {
    @JsonProperty("title")
    public abstract String title();

    @JsonProperty("type")
    public abstract Extractor.Type type();

    @JsonProperty("cursor_strategy")
    public abstract Extractor.CursorStrategy cursorStrategy();

    @JsonProperty("target_field")
    public abstract String targetField();

    @JsonProperty("source_field")
    public abstract String sourceField();

    @JsonProperty("configuration")
    public abstract Map<String, Object> configuration();

    @JsonProperty("converters")
    public abstract List<ConverterEntity> converters();

    @JsonProperty("condition_type")
    public abstract Extractor.ConditionType conditionType();

    @JsonProperty("condition_value")
    public abstract String conditionValue();

    @JsonProperty("order")
    public abstract int order();

    @JsonCreator
    public static ExtractorEntity create(
            @JsonProperty("title") String title,
            @JsonProperty("type") Extractor.Type type,
            @JsonProperty("cursor_strategy") Extractor.CursorStrategy cursorStrategy,
            @JsonProperty("target_field") String targetField,
            @JsonProperty("source_field") String sourceField,
            @JsonProperty("configuration") Map<String, Object> configuration,
            @JsonProperty("converters") List<ConverterEntity> converters,
            @JsonProperty("condition_type") Extractor.ConditionType conditionType,
            @JsonProperty("condition_value") String conditionValue,
            @JsonProperty("order") int order) {
        return new AutoValue_ExtractorEntity(
                title,
                type,
                cursorStrategy,
                targetField,
                sourceField,
                configuration,
                converters,
                conditionType,
                conditionValue,
                order);
    }
}
