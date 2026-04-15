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
package org.graylog2.rest.resources.system.outputs;

import org.graylog2.database.NotFoundException;
import org.graylog2.outputs.MessageOutputFactory;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.streams.Output;
import org.graylog2.rest.resources.streams.outputs.AvailableOutputSummary;
import org.graylog2.security.WithAuthorization;
import org.graylog2.security.WithAuthorizationExtension;
import org.graylog2.streams.OutputService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(WithAuthorizationExtension.class)
@WithAuthorization(permissions = {"*"})
class OutputResourceTest {

    @Mock
    OutputService outputService;

    @Mock
    MessageOutputFactory messageOutputFactory;

    @Mock
    Output existingOutput;

    OutputResource outputResource;

    @BeforeEach
    void setUp() {
        outputResource = new OutputResource(outputService, messageOutputFactory);
    }

    @Test
    void updateMergesConfigurationWithExistingValues() throws ValidationException, NotFoundException {
        final String outputId = "output-id-1";
        final String outputType = "org.graylog2.outputs.GelfOutput";

        // Existing output has port=12201 and hostname=graylog.example.com
        final Map<String, Object> existingConfig = Map.of(
                "hostname", "graylog.example.com",
                "port", 12201
        );
        when(existingOutput.getType()).thenReturn(outputType);
        when(existingOutput.getConfiguration()).thenReturn(existingConfig);
        when(outputService.load(outputId)).thenReturn(existingOutput);

        // Set up the output type's configuration request (defines known fields and types)
        final ConfigurationRequest configRequest = new ConfigurationRequest();
        configRequest.addField(new TextField("hostname", "Hostname", "", "Target hostname"));
        configRequest.addField(new NumberField("port", "Port", 0, "Target port",
                ConfigurationField.Optional.NOT_OPTIONAL));

        final AvailableOutputSummary outputSummary = AvailableOutputSummary.create(
                "GELF Output", outputType, "GELF Output", "", configRequest);
        when(messageOutputFactory.getAvailableOutputs()).thenReturn(Map.of(outputType, outputSummary));

        // Update only changes hostname, does NOT include port
        final Map<String, Object> deltas = new HashMap<>();
        deltas.put("configuration", new HashMap<>(Map.of("hostname", "new-host.example.com")));

        outputResource.update(outputId, deltas);

        // Verify that the saved configuration contains BOTH the updated hostname AND the preserved port
        @SuppressWarnings("unchecked")
        final ArgumentCaptor<Map<String, Object>> deltasCaptor = ArgumentCaptor.forClass(Map.class);
        verify(outputService).update(eq(outputId), deltasCaptor.capture());

        @SuppressWarnings("unchecked")
        final Map<String, Object> savedConfig = (Map<String, Object>) deltasCaptor.getValue().get("configuration");
        assertThat(savedConfig)
                .containsEntry("hostname", "new-host.example.com")
                .containsEntry("port", 12201);
    }

    @Test
    void updateOverridesExistingConfigurationValues() throws ValidationException, NotFoundException {
        final String outputId = "output-id-1";
        final String outputType = "org.graylog2.outputs.GelfOutput";

        final Map<String, Object> existingConfig = Map.of(
                "hostname", "old-host.example.com",
                "port", 12201
        );
        when(existingOutput.getType()).thenReturn(outputType);
        when(existingOutput.getConfiguration()).thenReturn(existingConfig);
        when(outputService.load(outputId)).thenReturn(existingOutput);

        final ConfigurationRequest configRequest = new ConfigurationRequest();
        configRequest.addField(new TextField("hostname", "Hostname", "", "Target hostname"));
        configRequest.addField(new NumberField("port", "Port", 0, "Target port",
                ConfigurationField.Optional.NOT_OPTIONAL));

        final AvailableOutputSummary outputSummary = AvailableOutputSummary.create(
                "GELF Output", outputType, "GELF Output", "", configRequest);
        when(messageOutputFactory.getAvailableOutputs()).thenReturn(Map.of(outputType, outputSummary));

        // Update changes both fields
        final Map<String, Object> deltas = new HashMap<>();
        deltas.put("configuration", new HashMap<>(Map.of("hostname", "new-host.example.com", "port", "9999")));

        outputResource.update(outputId, deltas);

        @SuppressWarnings("unchecked")
        final ArgumentCaptor<Map<String, Object>> deltasCaptor = ArgumentCaptor.forClass(Map.class);
        verify(outputService).update(eq(outputId), deltasCaptor.capture());

        @SuppressWarnings("unchecked")
        final Map<String, Object> savedConfig = (Map<String, Object>) deltasCaptor.getValue().get("configuration");
        assertThat(savedConfig)
                .containsEntry("hostname", "new-host.example.com")
                .containsEntry("port", 9999);
    }
}
