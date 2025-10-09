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
package org.graylog.events.processor.aggregation;

import com.github.rholder.retry.RetryException;
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.WebhookServerInstance;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.completebackend.FullBackendTest;
import org.graylog.testing.completebackend.GraylogBackendConfiguration;
import org.junit.jupiter.api.BeforeAll;

import java.util.concurrent.ExecutionException;

@GraylogBackendConfiguration(serverLifecycle = Lifecycle.CLASS)
public class EventNotificationsResourceIT {
    private static GraylogApis apis;
    private static WebhookServerInstance webhookServerInstance;


    @BeforeAll
    static void setUp(GraylogApis graylogApis) {
        apis = graylogApis;
        webhookServerInstance = apis.backend().getWebhookServerInstance().orElseThrow();
        graylogApis.system().urlAllowlist(webhookServerInstance.getContainerizedCollectorURI());
    }

    @FullBackendTest
    void testNotificationTestTrigger() throws ExecutionException, RetryException {

        final String httpNotificationID = apis.eventsNotifications().createHttpNotification(webhookServerInstance.getContainerizedCollectorURI());

        // now trigger the test of the notification, we should immediately see one recorded webhook afterward
        apis.post("/events/notifications/" + httpNotificationID + "/test", "", 200);

        // wait for the just triggered notification
        webhookServerInstance.waitForRequests(webhookRequest -> webhookRequest.body().contains("TEST_NOTIFICATION_ID"));

        // the wait succeeded, cleanup
        apis.eventsNotifications().deleteNotification(httpNotificationID);

    }
}
