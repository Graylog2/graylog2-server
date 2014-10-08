/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.rest.resources.system.inputs.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.Map;

@JsonAutoDetect
@AutoValue
public abstract class CreateExtractorRequest {
    @JsonProperty
    public abstract String title();

    @JsonProperty("cut_or_copy")
    public abstract String cutOrCopy();

    @JsonProperty("source_field")
    public abstract String sourceField();

    @JsonProperty("target_field")
    public abstract String targetField();

    @JsonProperty("extractor_type")
    public abstract String extractorType();

    @JsonProperty("extractor_config")
    public abstract Map<String, Object> extractorConfig();

    @JsonProperty
    public abstract Map<String, Map<String, Object>> converters();

    @JsonProperty("condition_type")
    public abstract String conditionType();

    @JsonProperty("condition_value")
    public abstract String conditionValue();

    @JsonProperty
    public abstract int order();

    public static CreateExtractorRequest create(@JsonProperty("title") @NotEmpty String title,
                                                @JsonProperty("cut_or_copy") String cutOrCopy,
                                                @JsonProperty("source_field") @NotEmpty String sourceField,
                                                @JsonProperty("target_field") @NotEmpty String targetField,
                                                @JsonProperty("extractor_type") @NotEmpty String extractorType,
                                                @JsonProperty("extractor_config") Map<String, Object> extractorConfig,
                                                @JsonProperty("converters") Map<String, Map<String, Object>> converters,
                                                @JsonProperty("condition_type") String conditionType,
                                                @JsonProperty("condition_value") String conditionValue,
                                                @JsonProperty("order") int order) {
        return new AutoValue_CreateExtractorRequest(title, cutOrCopy, sourceField, targetField, extractorType, extractorConfig, converters, conditionType, conditionValue, order);
    }
}
