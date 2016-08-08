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
