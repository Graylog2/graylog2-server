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
package org.graylog.integrations.pagerduty;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.events.notifications.EventNotificationContext;
import org.graylog.events.notifications.EventNotificationException;
import org.graylog.events.notifications.PermanentEventNotificationException;
import org.graylog.events.notifications.TemporaryEventNotificationException;
import org.graylog.integrations.pagerduty.client.MessageFactory;
import org.graylog.integrations.pagerduty.client.PagerDutyClient;
import org.graylog.integrations.pagerduty.dto.PagerDutyMessage;
import org.graylog.integrations.pagerduty.dto.PagerDutyResponse;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationImpl;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.plugin.system.SimpleNodeId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class PagerDutyNotificationTest {

    private PagerDutyNotification cut;

    // Mock Objects
    @Mock
    PagerDutyClient mockPagerDutyClient;
    @Mock
    MessageFactory mockMessageFactory;
    @Spy
    ObjectMapper spyObjectMapper;
    @Mock
    NotificationService mockNotificationService;

    // Test Objects
    EventNotificationContext ctx;
    Exception thrown;

    private final NodeId nodeId = new SimpleNodeId("5ca1ab1e-0000-4000-a000-000000000000");

    @Before
    public void setUp() throws Exception {
        cut = new PagerDutyNotification(mockPagerDutyClient, mockMessageFactory, spyObjectMapper, mockNotificationService, nodeId);
    }

    // Test Cases
    @Test
    public void execute_runsWithoutErrors_whenFoo() throws Exception {
        givenGoodContext();
        givenGoodMessageFactory();
        givenGoodObjectMapper();
        givenPagerDutyClientSucceeds();

        whenExecuteIsCalled();

        thenPagerDutyClientIsInvokedOnce();
    }

    @Test
    public void execute_throwsPermExceptionAndNotifies_whenClientThrowsPermPagerDutyClientException() throws Exception {
        givenGoodContext();
        givenGoodMessageFactory();
        givenGoodObjectMapper();
        givenGoodNotificationService();
        givenPagerDutyClientThrowsPermPagerDutyClientException();

        try {
            whenExecuteIsCalled();
        } catch (Exception e) {
            thrown = e;
        }

        thenNotificationSentToUI();
        thenPermanentEventNotificationExceptionIsThrown();
    }

    @Test(expected = TemporaryEventNotificationException.class)
    public void execute_throwsTempEventNotificationException_whenClientThrowsTempPagerDutyClientException() throws Exception {
        givenGoodContext();
        givenGoodMessageFactory();
        givenGoodObjectMapper();
        givenPagerDutyClientThrowsTempPagerDutyClientException();

        whenExecuteIsCalled();
    }

    @Test(expected = EventNotificationException.class)
    public void execute_throwsEventNotificationException_whenClientThrowsRuntimeException() throws Exception {
        givenGoodContext();
        givenGoodMessageFactory();
        givenGoodObjectMapper();
        givenPagerDutyClientThrowsRuntimeException();

        whenExecuteIsCalled();
    }

    // GIVENs
    private void givenGoodContext() {
        ctx = mock(EventNotificationContext.class);
    }

    private void givenGoodMessageFactory() {
        given(mockMessageFactory.createTriggerMessage(any(EventNotificationContext.class))).willReturn(mock(PagerDutyMessage.class));
    }

    private void givenGoodObjectMapper() throws IOException {
        given(spyObjectMapper.writeValueAsString(any(PagerDutyMessage.class))).willReturn("");
    }

    private void givenPagerDutyClientSucceeds() throws Exception {
        PagerDutyResponse response = mock(PagerDutyResponse.class);
        given(response.getErrors()).willReturn(List.of());
        given(mockPagerDutyClient.enqueue(any(String.class))).willReturn(response);
    }

    private void givenPagerDutyClientThrowsTempPagerDutyClientException() throws Exception {
        given(mockPagerDutyClient.enqueue(any(String.class))).willThrow(new PagerDutyClient.TemporaryPagerDutyClientException("test"));
    }

    private void givenPagerDutyClientThrowsPermPagerDutyClientException() throws Exception {
        given(mockPagerDutyClient.enqueue(any(String.class))).willThrow(new PagerDutyClient.PermanentPagerDutyClientException("test"));
    }

    private void givenPagerDutyClientThrowsRuntimeException() throws Exception {
        given(mockPagerDutyClient.enqueue(any(String.class))).willThrow(new RuntimeException("test"));
    }

    private void givenGoodNotificationService() {
        given(mockNotificationService.buildNow()).willReturn(new NotificationImpl().addTimestamp(Tools.nowUTC()));
    }

    // WHENs
    private void whenExecuteIsCalled() throws EventNotificationException {
        cut.execute(ctx);
    }

    // THENs
    private void thenPagerDutyClientIsInvokedOnce() throws Exception {
        ArgumentCaptor<String> pagerDutyMessageCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockPagerDutyClient, times(1)).enqueue(pagerDutyMessageCaptor.capture());

        assertThat(pagerDutyMessageCaptor.getValue(), notNullValue());
        assertThat(pagerDutyMessageCaptor.getValue(), is(""));
    }

    private void thenPermanentEventNotificationExceptionIsThrown() {
        assertThat(thrown, notNullValue());
        assertThat(thrown, instanceOf(PermanentEventNotificationException.class));
    }

    private void thenNotificationSentToUI() {
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(mockNotificationService, times(1)).publishIfFirst(notificationCaptor.capture());

        assertThat(notificationCaptor.getValue(), notNullValue());
        Notification sent = notificationCaptor.getValue();

        assertThat(sent.getType(), is(Notification.Type.GENERIC));
        assertThat(sent.getSeverity(), is(Notification.Severity.URGENT));
        assertThat(sent.getDetail("title"), is("PagerDuty Notification Failed"));
        assertThat(sent.getDetail("description"), notNullValue());
    }
}
