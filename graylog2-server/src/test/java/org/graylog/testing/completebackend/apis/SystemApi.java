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

import java.net.URI;
import java.util.Locale;

import static io.restassured.RestAssured.given;

public class SystemApi implements GraylogRestApi {
    private final GraylogApis api;

    public SystemApi(GraylogApis api) {
        this.api = api;
    }

    public void urlWhitelist(URI uri) {
        final String approvedUrlsReq = """
                {
                  "entries": [
                    {
                      "id": "4ac907a0-aa0f-463c-b828-bc6169339ab8",
                      "title": "local-webhook-tester",
                      "value": "%s",
                      "type": "literal"
                    }
                  ],
                  "disabled": false
                }
                """;
        api.put("/system/urlwhitelist", String.format(Locale.ROOT, approvedUrlsReq, uri), 204);
    }
}
