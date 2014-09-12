package org.graylog2.restclient.models.api.requests;

import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertNotNull;

public class AddStaticFieldRequestTest {
    @Test
    public void testToJson() {
        final AddStaticFieldRequest request = new AddStaticFieldRequest("key", "value");
        final String json = request.toJson();

        assertNotNull(json);
    }

}