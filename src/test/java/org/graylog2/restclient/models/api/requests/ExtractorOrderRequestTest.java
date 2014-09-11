package org.graylog2.restclient.models.api.requests;

import com.google.common.collect.ImmutableSortedMap;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertNotNull;

public class ExtractorOrderRequestTest {
    @Test
    public void testToJson() {
        final ExtractorOrderRequest request = new ExtractorOrderRequest();
        request.order = ImmutableSortedMap.of(1, "test");
        final String json = request.toJson();

        assertNotNull(json);
    }

}