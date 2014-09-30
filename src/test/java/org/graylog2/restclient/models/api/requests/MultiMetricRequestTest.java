package org.graylog2.restclient.models.api.requests;

import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertNotNull;

public class MultiMetricRequestTest {
    @Test
    public void testToJson() {
        final MultiMetricRequest request = new MultiMetricRequest();
        request.metrics = new String[]{"test"};
        final String json = request.toJson();

        assertNotNull(json);
    }

}