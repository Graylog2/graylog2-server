package org.graylog2.shared.rest.resources.system;

import org.graylog2.rest.models.system.metrics.requests.MetricsReadRequest;
import org.graylog2.rest.models.system.metrics.responses.MetricNamesResponse;
import org.graylog2.rest.models.system.metrics.responses.MetricsSummaryResponse;
import retrofit.Call;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public interface RemoteMetricsResource {
    @GET("/system/metrics/names")
    Call<MetricNamesResponse> metricNames();

    @POST("/system/metrics/multiple")
    Call<MetricsSummaryResponse> multipleMetrics(@Body @Valid @NotNull MetricsReadRequest request);

    @GET("/system/metrics/namespace/{namespace}")
    Call<MetricsSummaryResponse> byNamespace(@Path("namespace") String namespace);
}
