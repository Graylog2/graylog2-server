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

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.UniformReservoir;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.graylog.integrations.TestWithResources;
import org.graylog2.plugin.lookup.LookupResult;
import org.graylog2.security.encryption.EncryptedValueService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.Optional;

public class GreyNoiseCommunityIpLookupAdapterTest extends TestWithResources {

    @Mock
    private EncryptedValueService encryptedValueService;

    @Mock
    private GreyNoiseCommunityIpLookupAdapter.Config config;

    @Mock
    private OkHttpClient okHttpClient;

    @Mock
    private MetricRegistry metricRegistry;

    private Request request;

    private GreyNoiseCommunityIpLookupAdapter greyNoiseCommunityIpLookupAdapter;

    @Before
    public void setup() throws Exception {

        MockitoAnnotations.openMocks(this).close();
        request = new Request.Builder()
                .url(GreyNoiseCommunityIpLookupAdapter.GREYNOISE_COMMUNITY_ENDPOINT)
                .build();

        Mockito.when(metricRegistry.timer(Mockito.anyString())).thenReturn(new Timer(new UniformReservoir()));
        greyNoiseCommunityIpLookupAdapter = new GreyNoiseCommunityIpLookupAdapter("001", "test"
                , config, metricRegistry, encryptedValueService, okHttpClient);
    }

    @Test
    public void testParseIgnoredFields() {

        String string = getFileText("GreyNoiseCommunityIpLookupAdapter_test-parse-success.json");
        int statusCode = 200;

        Response response = createResponse(string, statusCode);
        LookupResult result = GreyNoiseCommunityIpLookupAdapter.parseResponse(response);

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        Assert.assertFalse("Result for status 200 not expected to have errors", result.hasError());
        Assert.assertNotNull(result.multiValue());
        Assert.assertFalse("Field 'message' is ignored and not expected in response", result.multiValue().containsKey("message"));
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
        assertValidMapWithKeys(result.multiValue(), "ip", "noise", "riot", "classification", "name", "link", "last_seen");
    }

    @Test
    public void testParse404() {

        String string = getFileText("GreyNoiseCommunityIpLookupAdapter_test-parse-404.json");
        int statusCode = 404;

        Response response = createResponse(string, statusCode);
        LookupResult result = GreyNoiseCommunityIpLookupAdapter.parseResponse(response);

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());

        assertValidMapWithKeys(result.multiValue(), "ip", "noise", "riot");

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
        assertValidMapWithKeys(result.multiValue(), "plan", "rate-limit", "plan_url");
    }

    @Test
    public void createRequestWithReservedIp() {

        String error = "Empty Request expected for Reserved IP Address";
        Assert.assertFalse(error, greyNoiseCommunityIpLookupAdapter.createRequest("127.0.0.1").isPresent());
        Assert.assertFalse(error, greyNoiseCommunityIpLookupAdapter.createRequest("192.168.1.100").isPresent());
        Assert.assertFalse(error, greyNoiseCommunityIpLookupAdapter.createRequest("10.1.1.100").isPresent());
        Assert.assertFalse(error, greyNoiseCommunityIpLookupAdapter.createRequest("172.16.1.100").isPresent());
    }

    @Test
    public void creatRequestWithIpv6Address() {
        Optional<Request> request = greyNoiseCommunityIpLookupAdapter.createRequest("2001::ffff:ffff:ffff:ffff:ffff:ffff");
        Assert.assertFalse("Empty Request expected for IPv6", request.isPresent());
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