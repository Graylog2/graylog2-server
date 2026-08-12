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
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.assertj.core.api.Assertions;
import org.graylog2.plugin.lookup.LookupResult;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.web.customization.CustomizationConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class GreyNoiseDataAdapterTest {

    Request mockRequest;
    Response mockResponse;
    String stringResponse;

    private GreyNoiseQuickIPDataAdapter adapter;

    @BeforeEach
    public void setUp() throws Exception {

        stringResponse = "{\"ip\":\"192.168.1.1\","
                + "\"business_service_intelligence\":{\"found\":true,\"trust_level\":\"1\"},"
                + "\"internet_scanner_intelligence\":{\"found\":true,\"classification\":\"malicious\"}}";

        mockRequest = new Request.Builder()
                .url("https://api.greynoise.io/v3/ip/")
                .build();

        final GreyNoiseQuickIPDataAdapter.Config config = GreyNoiseQuickIPDataAdapter.Config.builder()
                .type(GreyNoiseQuickIPDataAdapter.NAME)
                .apiToken(EncryptedValue.createUnset())
                .build();
        adapter = new GreyNoiseQuickIPDataAdapter("id", "name", config, new MetricRegistry(),
                mock(EncryptedValueService.class), mock(OkHttpClient.class), mock(CustomizationConfig.class),
                new ObjectMapper());
    }

    private void getvalidResponse() {
        mockResponse = new Response.Builder()
                .request(mockRequest)
                .protocol(Protocol.HTTP_2)
                .code(200)
                .message("")
                .body(ResponseBody.create(MediaType.get("application/json"), stringResponse))
                .build();
    }

    @Test
    public void parseBodyWithMultiValue(){
        getvalidResponse();

        final LookupResult result = adapter.parseResponse(mockResponse);
        assertThat(result, notNullValue());
        Assertions.assertThat(result.isEmpty()).isFalse();
        Assertions.assertThat(result.hasError()).isFalse();
        Assertions.assertThat(result.singleValue()).isEqualTo(null);
        Assertions.assertThat(result.multiValue()).isNotNull();
        Assertions.assertThat(result.multiValue().get("ip")).isEqualTo("192.168.1.1");
        Assertions.assertThat(result.multiValue().get("noise")).isEqualTo(true);
        Assertions.assertThat(result.multiValue().get("riot")).isEqualTo(true);
        Assertions.assertThat(result.multiValue().get("classification")).isEqualTo("malicious");
        Assertions.assertThat(result.multiValue().get("trust_level")).isEqualTo("1");
    }

}
