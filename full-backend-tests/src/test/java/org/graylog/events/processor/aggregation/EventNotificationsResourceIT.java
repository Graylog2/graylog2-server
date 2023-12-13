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

import org.assertj.core.api.Assertions;
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.WebhookServerInstance;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.junit.jupiter.api.BeforeEach;

@ContainerMatrixTestsConfiguration(serverLifecycle = Lifecycle.CLASS, searchVersions = {SearchServer.DATANODE_DEV}, withWebhookServerEnabled = true)
public class EventNotificationsResourceIT {
    private final GraylogApis graylogApis;
    private final WebhookServerInstance webhookTester;

    public EventNotificationsResourceIT(GraylogApis graylogApis) {
        this.graylogApis = graylogApis;
        webhookTester = graylogApis.backend().getWebhookServerInstance().orElseThrow(() -> new IllegalStateException("Webhook tester instance not found!"));
    }

    @BeforeEach
    void setUp() {
        graylogApis.system().urlWhitelist(webhookTester.getContainerizedCollectorURI());
    }

    @ContainerMatrixTest
    void testNotificationTestTrigger() {

        final String httpNotificationID = graylogApis.eventsNotifications().createHttpNotification(webhookTester.getContainerizedCollectorURI());

        // now trigger the test of the notification, we should immediately see one recorded webhook afterward
        graylogApis.post("/events/notifications/" + httpNotificationID + "/test", "", 200);

        Assertions.assertThat(webhookTester.allRequests())
                .hasSize(1)
                .allSatisfy((req) -> {
                    Assertions.assertThat(req.body()).contains("TEST_NOTIFICATION_ID");
                });

        graylogApis.eventsNotifications().deleteNotification(httpNotificationID);

    }
}
