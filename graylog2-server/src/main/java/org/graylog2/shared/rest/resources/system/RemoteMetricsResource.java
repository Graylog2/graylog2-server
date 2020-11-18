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
package org.graylog2.shared.rest.resources.system;

import org.graylog2.rest.models.system.metrics.requests.MetricsReadRequest;
import org.graylog2.rest.models.system.metrics.responses.MetricNamesResponse;
import org.graylog2.rest.models.system.metrics.responses.MetricsSummaryResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public interface RemoteMetricsResource {
    @GET("system/metrics/names")
    Call<MetricNamesResponse> metricNames();

    @POST("system/metrics/multiple")
    Call<MetricsSummaryResponse> multipleMetrics(@Body @Valid @NotNull MetricsReadRequest request);

    @GET("system/metrics/namespace/{namespace}")
    Call<MetricsSummaryResponse> byNamespace(@Path("namespace") String namespace);
}
