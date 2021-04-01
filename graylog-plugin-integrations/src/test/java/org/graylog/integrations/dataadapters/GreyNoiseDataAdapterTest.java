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
import org.assertj.core.api.Assertions;
import org.graylog2.plugin.lookup.LookupResult;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class GreyNoiseDataAdapterTest {

    Request mockRequest;
    Response mockResponse;
    String stringResponse;

    @Before
    public void setUp() throws Exception {

        stringResponse = "{\"ip\":\"192.168.1.1\",\"noise\":true,\"code\":\"0x01\"}";

        mockRequest = new Request.Builder()
                .url("https://api.greynoise.io/v2/noise/quick/")
                .build();
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

        final LookupResult result = GreyNoiseDataAdapter.parseResponse(mockResponse);
        assertThat(result, notNullValue());
        Assertions.assertThat(result.isEmpty()).isFalse();
        Assertions.assertThat(result.hasError()).isFalse();
        Assertions.assertThat(result.singleValue()).isEqualTo(null);
        Assertions.assertThat(result.multiValue()).isNotNull();
        Assertions.assertThat(result.multiValue().containsValue("192.168.1.1")).isTrue();
        Assertions.assertThat(result.multiValue().containsValue("0x01")).isTrue();
        Assertions.assertThat(result.multiValue().containsValue(true)).isTrue();
    }

}
