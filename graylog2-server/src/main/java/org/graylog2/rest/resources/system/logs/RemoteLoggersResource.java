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
package org.graylog2.rest.resources.system.logs;

import okhttp3.ResponseBody;
import org.graylog2.rest.models.system.loggers.responses.LoggersSummary;
import org.graylog2.rest.models.system.loggers.responses.SubsystemSummary;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Streaming;

public interface RemoteLoggersResource {
    @GET("system/loggers")
    Call<LoggersSummary> loggers();

    @GET("system/loggers/subsystems")
    Call<SubsystemSummary> subsystems();

    @PUT("system/loggers/subsystems/{subsystem}/level/{level}")
    Call<Void> setSubsystemLoggerLevel(@Path("subsystem") String subsystemTitle, @Path("level") String level);

    @PUT("system/loggers/{loggerName}/level/{level}")
    Call<Void> setSingleLoggerLevel(@Path("loggerName") String loggerName, @Path("level") String level);

    @GET("system/loggers/messages/recent")
    @Streaming
    @Headers({"Accept: text/plain"})
    Call<ResponseBody> messages(@Query("limit") int limit);
}
