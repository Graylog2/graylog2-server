package org.graylog2.restclient.models.api.requests;

import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertNotNull;

public class SessionCreateRequestTest {
    @Test
    public void testToJson() {
        final SessionCreateRequest request = new SessionCreateRequest("username", "password", "host");
        final String json = request.toJson();

        assertNotNull(json);
    }

}