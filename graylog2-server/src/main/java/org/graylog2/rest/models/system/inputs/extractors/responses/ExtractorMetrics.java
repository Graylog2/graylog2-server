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
package org.graylog2.rest.models.system.inputs.extractors.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.rest.models.metrics.responses.TimerRateMetricsResponse;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class ExtractorMetrics {
    @JsonProperty
    public abstract TimerRateMetricsResponse total();

    @JsonProperty
    public abstract TimerRateMetricsResponse condition();

    @JsonProperty
    public abstract TimerRateMetricsResponse execution();

    @JsonProperty
    public abstract TimerRateMetricsResponse converters();

    @JsonProperty
    public abstract long conditionHits();

    @JsonProperty
    public abstract long conditionMisses();

    public static ExtractorMetrics create(TimerRateMetricsResponse total,
                                          TimerRateMetricsResponse condition,
                                          TimerRateMetricsResponse execution,
                                          TimerRateMetricsResponse converters,
                                          long conditionHits,
                                          long conditionMisses) {
        return new AutoValue_ExtractorMetrics(total, condition, execution, converters, conditionHits, conditionMisses);
    }
}
