/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.integrations.aws.transports;

import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.inputs.MisfireException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
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

        assertThatThrownBy(() -> KinesisTransport.validateEndpoint("unknown-scheme://graylog.org", "Graylog"))
                .isExactlyInstanceOf(MisfireException.class)
                .hasMessageContaining("Override Endpoint")
                .hasMessageContaining("is invalid");
    }

    @Test
    public void testConfigContainsSingleTableStateTrackingField() {
        final ConfigurationRequest request = new KinesisTransport.Config().getRequestedConfiguration();

        assertThat(request.containsField(KinesisTransport.CK_KINESIS_SINGLE_TABLE_STATE_TRACKING)).isTrue();

        final ConfigurationField field = request.getField(KinesisTransport.CK_KINESIS_SINGLE_TABLE_STATE_TRACKING);
        assertThat(field).isInstanceOf(BooleanField.class);
        // New inputs should default to the single-table layout.
        assertThat(field.getDefaultValue()).isEqualTo(true);
    }
}
