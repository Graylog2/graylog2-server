package org.graylog2.restclient.models.api.requests;

import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertNotNull;

public class CreateUserRequestFormTest {
    @Test
    public void testToJson() {
        final CreateUserRequestForm request = new CreateUserRequestForm();
        request.setUsername("username");
        final String json = request.toJson();

        assertNotNull(json);
    }

}