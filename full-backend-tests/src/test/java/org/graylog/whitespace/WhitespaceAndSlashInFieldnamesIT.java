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
package org.graylog.whitespace;

import com.github.rholder.retry.RetryException;
import org.assertj.core.api.Assertions;
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.WebhookRequest;
import org.graylog.testing.completebackend.WebhookServerInstance;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutionException;

@ContainerMatrixTestsConfiguration(serverLifecycle = Lifecycle.CLASS, searchVersions = {SearchServer.ES7, SearchServer.OS2_LATEST, SearchServer.DATANODE_DEV}, withWebhookServerEnabled = true)
public class WhitespaceAndSlashInFieldnamesIT {
    private static final Logger LOG = LoggerFactory.getLogger(WhitespaceAndSlashInFieldnamesIT.class);
    private static final String indexSetPrefix = "ws-search-test";

    private final GraylogApis apis;
    private final WebhookServerInstance webhookTester;
    private String indexSetId;

    public WhitespaceAndSlashInFieldnamesIT(GraylogApis apis) {
        this.apis = apis;
        this.webhookTester = apis.backend().getWebhookServerInstance().orElseThrow(() -> new IllegalStateException("Webhook tester not found!"));
    }

    @BeforeEach
    void setUp() {
        this.indexSetId = apis.indices().createIndexSet("Whitespace/Slash Test", "", indexSetPrefix);
        apis.indices().waitFor(() -> apis.backend().searchServerInstance().client().indicesExists(indexSetPrefix + "_0", indexSetPrefix + "_deflector"),
                "Timed out waiting for index/deflector to be created.");
    }

    @AfterEach
    void tearDown() {
        apis.indices().deleteIndexSet(this.indexSetId, true);
        apis.indices().waitFor(() -> !apis.backend().searchServerInstance().client().indicesExists(indexSetPrefix + "_0")
                        && !apis.backend().searchServerInstance().client().indicesExists(indexSetPrefix + "_deflector"),
                "Timed out waiting for index/deflector to be deleted.");
    }

    @ContainerMatrixTest
    void testPivotAggregationSearchAllKnownFields() throws ExecutionException, RetryException {
        apis.system().urlWhitelist(webhookTester.getContainerizedCollectorURI());

        final String notificationID = apis.eventsNotifications().createHttpNotification(webhookTester.getContainerizedCollectorURI());

        final String eventDefinitionID = apis.eventDefinitions().createEventDefinition(notificationID, List.of(
                "http_response_code",
                "whitespace field",
                "slash/field",
                "type"
        ));

        postMessages();

        waitForWebHook(eventDefinitionID, "my alert def: 200|201|202|ssh - count()=3.0");

        apis.eventsNotifications().deleteNotification(notificationID);
        apis.eventDefinitions().deleteDefinition(eventDefinitionID);
    }

    @ContainerMatrixTest
    void testPivotAggregationSearchOneUnknownField() throws ExecutionException, RetryException {
        apis.system().urlWhitelist(webhookTester.getContainerizedCollectorURI());

        final String notificationID = apis.eventsNotifications().createHttpNotification(webhookTester.getContainerizedCollectorURI());

        final String eventDefinitionID = apis.eventDefinitions().createEventDefinition(notificationID, List.of(
                "http_response_code",
                "unknown field",
                "type"
        ));

        postMessages();

        waitForWebHook(eventDefinitionID, "my alert def: 200|(Empty Value)|ssh - count()=3.0");

        apis.eventsNotifications().deleteNotification(notificationID);
        apis.eventDefinitions().deleteDefinition(eventDefinitionID);
    }

    @ContainerMatrixTest
    void testPivotAggregationSearchAllUnknownFields() throws ExecutionException, RetryException {
        apis.system().urlWhitelist(webhookTester.getContainerizedCollectorURI());

        final String notificationID = apis.eventsNotifications().createHttpNotification(webhookTester.getContainerizedCollectorURI());

        final String eventDefinitionID = apis.eventDefinitions().createEventDefinition(notificationID, List.of(
                "unknown_field_1",
                "unknown/field_2",
                "unknown field_3"
        ));

        postMessages();

        waitForWebHook(eventDefinitionID, "my alert def: (Empty Value)|(Empty Value)|(Empty Value) - count()=3.0");

        apis.eventsNotifications().deleteNotification(notificationID);
        apis.eventDefinitions().deleteDefinition(eventDefinitionID);
    }

    private void waitForWebHook(String eventDefinitionID, String eventMessage) throws ExecutionException, RetryException {
        try {
            final List<WebhookRequest> requests = webhookTester.waitForRequests((req) -> req.bodyAsJsonPath().read("event_definition_id").equals(eventDefinitionID));
            Assertions.assertThat(requests)
                    .isNotEmpty()
                    .allSatisfy(req -> {
                        final String message = req.bodyAsJsonPath().read("event.message");
                        Assertions.assertThat(message).isEqualTo(eventMessage);
                    });

        } catch (ExecutionException | RetryException e) {
            LOG.error(this.apis.backend().getLogs());
            throw e;
        }
    }

    private void postMessages() {
        apis.gelf().createGelfHttpInput()
                .postMessage("""
                        {
                        "short_message":"whitesapce-slash-test-1",
                        "host":"example.org",
                        "type":"ssh",
                        "source":"example.org",
                        "http_response_code":200,
                        "whitespace field":201,
                        "slash/field":202,
                        "resource": "posts"
                        }""")
                .postMessage("""
                        {
                        "short_message":"whitesapce-slash-test-2",
                        "host":"example.org",
                        "type":"ssh",
                        "source":"example.org",
                        "http_response_code":200,
                        "whitespace field":201,
                        "slash/field":202,
                        "resource": "posts"
                        }""")
                .postMessage("""
                        {
                        "short_message":"whitesapce-slash-test-3",
                        "host":"example.org",
                        "type":"ssh",
                        "source":"example.org",
                        "http_response_code":200,
                        "whitespace field":201,
                        "slash/field":202,
                        "resource": "posts"
                        }""");
        apis.search().waitForMessagesCount(3);
        apis.backend().searchServerInstance().client().refreshNode();
    }
}
