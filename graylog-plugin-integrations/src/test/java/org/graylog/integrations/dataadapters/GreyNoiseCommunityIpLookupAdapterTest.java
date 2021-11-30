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
package org.graylog.integrations.dataadapters;

import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.graylog.integrations.TestWithResources;
import org.graylog2.plugin.lookup.LookupResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

public class GreyNoiseCommunityIpLookupAdapterTest extends TestWithResources {

    private Request request;

    @Before
    public void setup() {

        request = new Request.Builder()
                .url(GreyNoiseCommunityIpLookupAdapter.GREYNOISE_COMMUNITY_ENDPOINT)
                .build();
    }

    @Test
    public void testParseSuccess() {

        String string = getFileText("GreyNoiseCommunityIpLookupAdapter_test-parse-success.json");
        int statusCode = 200;

        Response response = createResponse(string, statusCode);
        LookupResult result = GreyNoiseCommunityIpLookupAdapter.parseResponse(response);

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        Assert.assertFalse("Result for status 200 not expected to have errors", result.hasError());
        assertValidMapWithKeys(result.multiValue(), "ip", "noise", "riot", "message", "classification", "name", "link", "last_seen", "message");
    }

    @Test
    public void testParse404() {

        String string = getFileText("GreyNoiseCommunityIpLookupAdapter_test-parse-404.json");
        int statusCode = 404;

        Response response = createResponse(string, statusCode);
        LookupResult result = GreyNoiseCommunityIpLookupAdapter.parseResponse(response);

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());

        assertValidMapWithKeys(result.multiValue(), "ip", "noise", "riot", "message");

        Assert.assertFalse("Invalid Result. A 404 response should not be considered an error", result.hasError());
    }

    @Test
    public void testRateLimitReached() {

        String string = getFileText("GreyNoiseCommunityIpLookupAdapter_test-parse-LimitReached.json");
        int statusCode = 429;

        Response response = createResponse(string, statusCode);
        LookupResult result = GreyNoiseCommunityIpLookupAdapter.parseResponse(response);

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        Assert.assertTrue("Result for status 429 expected to have errors", result.hasError());
        assertValidMapWithKeys(result.multiValue(), "plan", "rate-limit", "plan_url", "message");
    }

    private Response createResponse(String responseBody, int statusCode) {
        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_2)
                .code(statusCode)
                .message("")
                .body(ResponseBody.create(MediaType.get("application/json"), responseBody))
                .build();
    }

    private void assertValidMapWithKeys(Map<Object, Object> map, Object... expectedKeys) {

        Assert.assertNotNull("Invalid result values map (NULL)", map);

        for (Object key : expectedKeys) {
            String error = String.format("Key [%s] not found in values map", key);
            Assert.assertTrue(error, map.containsKey(key));
        }
    }
}