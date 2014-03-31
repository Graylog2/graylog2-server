/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
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
 *
 */
package org.graylog2.rest.resources.system.inputs.requests;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class CreateExtractorRequest {

    public String title;

    @JsonProperty("cut_or_copy")
    public String cutOrCopy;

    @JsonProperty("source_field")
    public String sourceField;

    @JsonProperty("target_field")
    public String targetField;

    @JsonProperty("extractor_type")
    public String extractorType;

    @JsonProperty("creator_user_id")
    public String creatorUserId;

    @JsonProperty("extractor_config")
    public Map<String, Object> extractorConfig;

    public Map<String, Map<String, Object>> converters;

    @JsonProperty("condition_type")
    public String conditionType;

    @JsonProperty("condition_value")
    public String conditionValue;

    public int order;

}
