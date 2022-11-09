package org.graylog.testing.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.restassured.specification.RequestSpecification;

import java.util.concurrent.atomic.AtomicInteger;

public class TestGelfInput {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RequestSpecification requestSpecification;
    private final int mappedPort;
    private final AtomicInteger messageCounter = new AtomicInteger(0);

    public TestGelfInput(RequestSpecification requestSpecification, int mappedPort) {
        this.requestSpecification = requestSpecification;
        this.mappedPort = mappedPort;
    }


    public TestGelfInput postMessage(ImmutableMap<String, Object> message) throws JsonProcessingException {
        GelfInputUtils.postMessage(mappedPort, OBJECT_MAPPER.writeValueAsString(message), requestSpecification);
        messageCounter.incrementAndGet();
        return this;
    }

    public TestGelfInput postMessage(String message) {
        GelfInputUtils.postMessage(mappedPort, message, requestSpecification);
        messageCounter.incrementAndGet();
        return this;
    }

    /**
     * Caution, this may cause problems if any other messages already exist in the index!
     */
    public void waitForAllMessages() {
        final int expectedMessagesCount = messageCounter.get();
        WaitUtils.waitFor(() -> SearchUtils.searchForAllMessages(requestSpecification).size() >= expectedMessagesCount, "Failed to wait for messages count:" + expectedMessagesCount);
    }
}
