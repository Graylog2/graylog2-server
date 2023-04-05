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
package org.graylog2.rest.resources.system.debug.bundle;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Streaming;

import java.util.List;

public interface RemoteSupportBundleInterface {
    @GET("system/debug/support/manifest")
    Call<SupportBundleNodeManifest> getNodeManifest();

    @POST("system/debug/support/bundle/build")
    Call<Void> buildSupportBundle();

    @GET("system/debug/support/logfile/{id}")
    @Streaming
    @Headers({"Accept: */*"})
    Call<ResponseBody> getLogFile(@Path("id") String id);

    @GET("system/debug/support/bundle/list")
    Call<List<BundleFile>> listBundles();

    @GET("system/debug/support/bundle/download/{filename}")
    @Streaming
    @Headers({"Accept: application/octet-stream"})
    Call<ResponseBody> downloadBundle(@Path("filename") String filename);

    @DELETE("system/debug/support/bundle/{filename}")
    Call<Void> deleteBundle(@Path("filename") String filename);
}
