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

import retrofit2.Call;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface RemoteDeflectorResource {
    @POST("system/deflector/cycle")
    Call<Void> cycle();

    @POST("system/deflector/{indexSetId}/cycle")
    Call<Void> cycleIndexSet(@Path("indexSetId") String indexSetId);
}
