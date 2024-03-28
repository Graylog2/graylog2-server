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
package org.graylog.plugins.views.search.rest.remote;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import retrofit2.http.Streaming;

public interface RemoteSearchJobsStatusInterface {

    @GET("views/search/status/{jobId}")
    @Streaming
    @Headers({"Accept: */*"})
        //Call<ResponseBody> response is used intentionally instead of Call<SearchJobDTO>, because we do not want unnecessary serialization/deserialization of response we just pass between nodes.
        //What is more, SearchJobDTO is not supporting deserialization right now and it would require a significant amount of work to change that.
    Call<ResponseBody> jobStatus(@Path("jobId") String jobId);

    @DELETE("views/search/cancel/{jobId}")
    Call<Void> cancelJob(@Path("jobId") String jobId);
}
