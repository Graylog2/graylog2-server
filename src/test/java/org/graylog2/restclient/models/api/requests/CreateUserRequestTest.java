package org.graylog2.restclient.models.api.requests;

import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertNotNull;

public class CreateUserRequestTest {
    @Test
    public void testToJson() {
        final CreateUserRequest request = new CreateUserRequest();
        request.setUsername("username");
        final String json = request.toJson();

        assertNotNull(json);
    }

}