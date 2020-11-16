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
package org.graylog2.rest.resources.system.jobs;

import org.graylog2.rest.models.system.SystemJobSummary;
import org.graylog2.rest.models.system.jobs.requests.TriggerRequest;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

import java.util.List;
import java.util.Map;

public interface RemoteSystemJobResource {
    @GET("system/jobs")
    Call<Map<String, List<SystemJobSummary>>> list();

    @GET("system/jobs/{jobId}")
    Call<SystemJobSummary> get(@Path("jobId") String jobId);

    @DELETE("system/jobs/{jobId}")
    Call<SystemJobSummary> delete(@Path("jobId") String jobId);

    @POST("system/jobs")
    Call trigger(@Body TriggerRequest tr);
}
