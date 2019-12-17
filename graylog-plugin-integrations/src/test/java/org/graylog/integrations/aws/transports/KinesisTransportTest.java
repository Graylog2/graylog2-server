package org.graylog.integrations.aws.transports;

import org.graylog2.plugin.inputs.MisfireException;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class KinesisTransportTest {

    @Test
    public void testValidateEndpoint() throws MisfireException {

        // Validate that no exception occurs for valid URI.
        KinesisTransport.validateEndpoint("https://graylog.org", "Graylog");

        // Validate that no exception occurs for blank and null URL.
        KinesisTransport.validateEndpoint("", "Blank");
        KinesisTransport.validateEndpoint(null, "Null");

        // Verify exception occurs for invalid URI.
        assertThatThrownBy(() -> KinesisTransport.validateEndpoint("haha not a url", "Bad URI"))
                .isExactlyInstanceOf(MisfireException.class)
                .hasMessageContaining("Override Endpoint")
                .hasMessageContaining("is invalid");
    }
}