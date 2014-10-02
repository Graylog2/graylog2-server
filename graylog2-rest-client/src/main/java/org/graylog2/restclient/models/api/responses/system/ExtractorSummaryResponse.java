/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
