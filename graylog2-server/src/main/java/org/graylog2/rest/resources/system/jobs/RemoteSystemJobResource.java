package org.graylog2.rest.resources.system.jobs;

import org.graylog2.rest.models.system.SystemJobSummary;
import org.graylog2.rest.models.system.jobs.requests.TriggerRequest;
import retrofit.Call;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;

import java.util.List;
import java.util.Map;

public interface RemoteSystemJobResource {
    @GET("/system/jobs")
    Call<Map<String, List<SystemJobSummary>>> list();

    @GET("/system/jobs/{jobId}")
    Call<SystemJobSummary> get(String jobId);

    @POST("/system/jobs")
    Call trigger(@Body TriggerRequest tr);
}
