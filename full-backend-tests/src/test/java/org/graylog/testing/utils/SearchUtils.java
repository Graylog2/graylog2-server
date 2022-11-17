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
package org.graylog.testing.utils;

import io.restassured.specification.RequestSpecification;
import org.graylog.plugins.views.search.rest.MappedFieldTypeDTO;
import org.graylog.testing.backenddriver.SearchDriver;
import org.graylog2.rest.models.messages.responses.ResultMessageSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class SearchUtils {

    private SearchUtils() {
    }

    public static List<String> searchForAllMessages(RequestSpecification requestSpecification) {
        List<String> messages = new ArrayList<>();

        WaitUtils.waitFor(() -> captureMessages(list -> list.stream().map(m -> (String) m.message().get("message")).forEach(messages::add), requestSpecification), "Timed out waiting for messages to be present");

        return messages;
    }

    public static boolean waitForMessage(RequestSpecification requestSpecification, String message) {
        WaitUtils.waitFor(() -> captureMessage(requestSpecification, m -> message.equals(m.message().get("message"))), "Timed out waiting for message to be present");
        return true;
    }

    public static boolean waitForMessage(RequestSpecification requestSpecification, Predicate<ResultMessageSummary> messageFilter) {
        AtomicReference<ResultMessageSummary> messageReference = new AtomicReference<>();
        WaitUtils.waitFor(() -> captureMessage(requestSpecification, messageFilter), "Timed out waiting for message to be present");
        return true;
    }

    private static boolean captureMessage(RequestSpecification requestSpecification, Predicate<ResultMessageSummary> messageFilter) {
        return SearchDriver.searchAllMessages(requestSpecification).stream().anyMatch(messageFilter);
    }

    private static boolean captureMessages(Consumer<List<ResultMessageSummary>> messagesCaptor,
                                           RequestSpecification requestSpecification) {
        List<ResultMessageSummary> messages = SearchDriver.searchAllMessages(requestSpecification);
        if (!messages.isEmpty()) {
            messagesCaptor.accept(messages);
            return true;
        }
        return false;
    }

    public static MappedFieldTypeDTO waitForFieldTypeDefinition(RequestSpecification requestSpecification, String fieldName) {
        return WaitUtils.waitForObject(() -> SearchDriver.getFieldTypes(requestSpecification)
                        .stream()
                        .filter(t -> t.name().equals(fieldName))
                        .findFirst()
                , "Timed out waiting for field definition");
    }
}
