package org.graylog2.restclient.models.api.requests;

import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertNotNull;

public class InputLaunchRequestTest {
    @Test
    public void testToJson() {
        final InputLaunchRequest request = new InputLaunchRequest();
        request.title = "title";
        final String json = request.toJson();

        assertNotNull(json);
    }

}