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
package org.graylog2.telemetry.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface PosthogAPI {
    record BatchRequest(@JsonProperty("api_key") String apiKey, @JsonProperty("batch") Collection<Event> batch) {}

    record Event(@JsonProperty("uuid") String uuid,
                 @JsonProperty("timestamp") String timestamp,
                 @JsonProperty("distinct_id") String distinctId,
                 @JsonProperty("event") String event,
                 @JsonProperty("properties") Map<String, Object> properties) {
        public static Event create(String distinctId, String event, Map<String, Object> properties) {
            return new Event(UUID.randomUUID().toString(), Instant.now().toString(), distinctId, event, properties);
        }
    }

    @POST
    Call<Void> batchSend(@Body BatchRequest batchRequest);
}
