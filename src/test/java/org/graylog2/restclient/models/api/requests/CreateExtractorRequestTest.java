package org.graylog2.restclient.models.api.requests;

import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertNotNull;

public class CreateExtractorRequestTest {
    @Test
    public void testToJson() {
        final CreateExtractorRequest request = new CreateExtractorRequest();
        request.title = "title";
        final String json = request.toJson();

        assertNotNull(json);
    }

}