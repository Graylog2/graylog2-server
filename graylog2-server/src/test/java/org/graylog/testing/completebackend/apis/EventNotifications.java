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
package org.graylog.testing.completebackend.apis;

import io.restassured.response.ValidatableResponse;

import java.net.URI;
import java.util.Locale;

public class EventNotifications implements GraylogRestApi {
    private final GraylogApis api;

    public EventNotifications(GraylogApis api) {
        this.api = api;
    }

    public void deleteNotification(String notificationID) {
        api.delete("/events/notifications/" + notificationID, 204);
    }

    public String createHttpNotification(URI uri) {
        final String body = """
                {
                  "title": "my http notification",
                  "description": "",
                  "config": {
                    "url": "%s",
                    "api_key_as_header": false,
                    "api_key": "",
                    "api_secret": null,
                    "basic_auth": null,
                    "skip_tls_verification": false,
                    "type": "http-notification-v1"
                  }
                }
                """;
        final ValidatableResponse res = api.post("/events/notifications", String.format(Locale.ROOT, body, uri), 200);
        return res.extract().body().jsonPath().getString("id");
    }
}
