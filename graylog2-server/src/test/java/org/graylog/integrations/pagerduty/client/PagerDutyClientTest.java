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
package org.graylog.integrations.pagerduty.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.graylog.integrations.pagerduty.dto.PagerDutyResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class PagerDutyClientTest {

    // Code Under Test
    @InjectMocks
    private PagerDutyClient cut;

    // Mock Objects
    @Mock
    private OkHttpClient mockHttpClient;

    @Spy
    private ObjectMapper spyObjectMapper;

    // Test Objects
    private String messagePayload;
    private PagerDutyResponse pagerDutyResponse;

    // Test constants
    private static final String TEST_MESSAGE = "This is a test of the Emergency Broadcast System.";
    private static final String GOOD_API_RESPONSE = "{\"status\":\"\", \"message\":\"\", \"dedup_key\":\"\", \"errors\":[]}";
    private static final String ERROR_API_RESPONSE = "{\"status\":\"\", \"message\":\"\", \"dedup_key\":\"\", \"errors\":[\"error\"]}";

    // Test Cases
    @Test
    public void enqueue_returnsSuccessfulResponse_whenAPICallSucceeds() throws Exception {
        givenGoodMessagePayload();
        givenApiCallSucceeds();

        whenEnqueueIsCalled();

        thenGoodRequestSentToAPI();
        thenGoodResponseReturned();
    }

    @Test
    public void enqueue_returnsErrorResponse_whenAPICallReturnsErrors() throws Exception {
        givenGoodMessagePayload();
        givenApiCallReturnsErrors();

        whenEnqueueIsCalled();

        thenGoodRequestSentToAPI();
        thenErrorResponseReturned();
    }

    @Test(expected = PagerDutyClient.TemporaryPagerDutyClientException.class)
    public void enqueue_throwsTempPagerDutyClientException_whenServerError() throws Exception {
        givenGoodMessagePayload();
        givenApiCallFailsDueToServerError();

        whenEnqueueIsCalled();
    }

    @Test(expected = PagerDutyClient.TemporaryPagerDutyClientException.class)
    public void enqueue_throwsTempPagerDutyClientException_whenTooManyAPICalls() throws Exception {
        givenGoodMessagePayload();
        givenApiCallFailsDueToTooManyCalls();

        whenEnqueueIsCalled();
    }

    @Test(expected = PagerDutyClient.PermanentPagerDutyClientException.class)
    public void enqueue_throwsPermPagerDutyClientException_whenBadRequest() throws Exception {
        givenGoodMessagePayload();
        givenApiCallFailsDueToBadInput();

        whenEnqueueIsCalled();
    }

    // GIVENs
    private void givenGoodMessagePayload() {
        messagePayload = TEST_MESSAGE;
    }

    private void givenApiCallSucceeds() throws Exception {
        Response response = buildResponse(202, GOOD_API_RESPONSE);
        Call mockCall = mock(Call.class);
        given(mockCall.execute()).willReturn(response);
        given(mockHttpClient.newCall(any(Request.class))).willReturn(mockCall);
    }

    private void givenApiCallReturnsErrors() throws Exception {
        Response response = buildResponse(202, ERROR_API_RESPONSE);
        Call mockCall = mock(Call.class);
        given(mockCall.execute()).willReturn(response);
        given(mockHttpClient.newCall(any(Request.class))).willReturn(mockCall);
    }

    private void givenApiCallFailsDueToBadInput() throws Exception {
        Response response = buildResponse(400, "bad routing key");
        Call mockCall = mock(Call.class);
        given(mockCall.execute()).willReturn(response);
        given(mockHttpClient.newCall(any(Request.class))).willReturn(mockCall);
    }

    private void givenApiCallFailsDueToTooManyCalls() throws Exception {
        Response response = buildResponse(429, "");
        Call mockCall = mock(Call.class);
        given(mockCall.execute()).willReturn(response);
        given(mockHttpClient.newCall(any(Request.class))).willReturn(mockCall);
    }

    private void givenApiCallFailsDueToServerError() throws Exception {
        Response response = buildResponse(500, "");
        Call mockCall = mock(Call.class);
        given(mockCall.execute()).willReturn(response);
        given(mockHttpClient.newCall(any(Request.class))).willReturn(mockCall);
    }

    // WHENs
    private void whenEnqueueIsCalled() throws Exception {
        pagerDutyResponse = cut.enqueue(messagePayload);
    }

    // THENs
    private void thenGoodRequestSentToAPI() throws Exception {
        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mockHttpClient, times(1)).newCall(requestCaptor.capture());

        assertThat(requestCaptor.getValue(), notNullValue());
        Request request = requestCaptor.getValue();

        assertThat(request.url().toString(), is(PagerDutyClient.API_URL));
        assertThat(request.method(), is("POST"));
        assertThat(request.body(), notNullValue());
        assertThat(request.body().contentLength(), is(Long.valueOf(TEST_MESSAGE.length())));
        Buffer buffer = new Buffer();
        request.body().writeTo(buffer);
        assertThat(buffer.readUtf8(), is(TEST_MESSAGE));
    }

    private void thenGoodResponseReturned() {
        assertThat(pagerDutyResponse, notNullValue());
        assertThat(pagerDutyResponse.getErrors().size(), is(0));
    }

    private void thenErrorResponseReturned() {
        assertThat(pagerDutyResponse, notNullValue());
        assertThat(pagerDutyResponse.getErrors().size(), is(1));
    }

    // Utility Methods
    private Response buildResponse(int httpResponseCode, String responseBody) {
        return new Response.Builder()
                .request(new Request.Builder()
                        .url("https://events.pagerduty.com/v2/enqueue")
                        .build())
                .protocol(Protocol.HTTP_2)
                .code(httpResponseCode)
                .message("")
                .body(ResponseBody.create(MediaType.parse(APPLICATION_JSON), responseBody))
                .build();
    }
}
