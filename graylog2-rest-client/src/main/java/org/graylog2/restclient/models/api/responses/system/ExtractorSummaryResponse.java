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
package org.graylog2.restclient.models.api.responses.system;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.restclient.models.api.responses.metrics.TimerRateMetricsResponse;

import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class ExtractorSummaryResponse {

    public String id;
    public String title;
    public String type;
    public int order;

    @JsonProperty("target_field")
    public String targetField;

    @JsonProperty("source_field")
    public String sourceField;

    @JsonProperty("cursor_strategy")
    public String cursorStrategy;

    @JsonProperty("extractor_config")
    public Map<String, Object> extractorConfig;

    @JsonProperty("creator_user_id")
    public String creatorUserId;

    public List<Map<String, Object>> converters;

    @JsonProperty("condition_type")
    public String conditionType;

    @JsonProperty("condition_value")
    public String conditionValue;

    public long exceptions;

    @JsonProperty("converter_exceptions")
    public long converterExceptions;

    public Map<String, TimerRateMetricsResponse> metrics;

}
