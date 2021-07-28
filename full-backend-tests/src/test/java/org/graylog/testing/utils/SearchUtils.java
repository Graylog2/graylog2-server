package org.graylog.testing.utils;

import io.restassured.specification.RequestSpecification;
import org.graylog.testing.backenddriver.SearchDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class SearchUtils {

    private SearchUtils() {}

    public static List<String> searchForAllMessages(RequestSpecification requestSpecification) {
        List<String> messages = new ArrayList<>();

        WaitUtils.waitFor(() -> captureMessages(messages::addAll, requestSpecification), "Timed out waiting for messages to be present");

        return messages;
    }

    private static boolean captureMessages(Consumer<List<String>> messagesCaptor,
                                           RequestSpecification requestSpecification) {
        List<String> messages = SearchDriver.searchAllMessages(requestSpecification);
        if (!messages.isEmpty()) {
            messagesCaptor.accept(messages);
            return true;
        }
        return false;
    }
}
