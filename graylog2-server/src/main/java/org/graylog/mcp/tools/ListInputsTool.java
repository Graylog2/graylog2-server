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
package org.graylog.mcp.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.base.Strings;
import jakarta.inject.Inject;
import org.graylog.mcp.server.Tool;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.rest.models.system.inputs.responses.InputSummary;
import org.graylog2.shared.inputs.InputDescription;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.graylog2.shared.security.RestPermissions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ListInputsTool extends Tool<ListInputsTool.Parameters, String> {
    public static String NAME = "list_inputs";

    private final InputService inputService;
    private final Map<String, InputDescription> availableInputs;

    @Inject
    public ListInputsTool(ObjectMapper objectMapper, InputService inputService, MessageInputFactory messageInputFactory) {
        super(objectMapper,
                new TypeReference<>() {},
                NAME,
                "List Graylog Inputs",
                """
                        List all configured Graylog inputs. Returns detailed information about each input including type (syslog, GELF, etc.), current state (running/stopped),
                        configuration parameters, and throughput statistics. Use this to monitor input health, troubleshoot data ingestion issues, or understand what types of
                        logs are being collected. No parameters required. Returns JSON-formatted input details..
                        """);
        this.inputService = inputService;
        this.availableInputs = messageInputFactory.getAvailableInputs();
    }

    @Override
    public String apply(PermissionHelper permissionHelper, ListInputsTool.Parameters unused) {
        try (java.util.stream.Stream<Input> inputs = inputService.all().stream()) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JodaModule());
            return mapper.writeValueAsString(
                    inputs.filter(input -> permissionHelper.isPermitted(RestPermissions.INPUTS_READ, input.getId()))
                            .map(input -> {
                                final InputDescription inputDescription = this.availableInputs.get(input.getType());
                                final ConfigurationRequest configurationRequest = inputDescription != null ? inputDescription.getConfigurationRequest() : null;
                                final Map<String, Object> configuration = permissionHelper.isPermitted(RestPermissions.INPUTS_EDIT, input.getId()) && permissionHelper.isPermitted(RestPermissions.INPUT_TYPES_CREATE, input.getType()) ?
                                        input.getConfiguration() : maskPasswordsInConfiguration(input.getConfiguration(), configurationRequest);
                                return InputSummary.create(input.getTitle(),
                                        input.isGlobal(),
                                        InputDescription.getInputDescriptionName(inputDescription, input.getType()),
                                        input.getContentPack(),
                                        input.getId(),
                                        input.getCreatedAt(),
                                        input.getType(),
                                        input.getCreatorUserId(),
                                        configuration,
                                        input.getStaticFields(),
                                        input.getNodeId());
                            })
                            .toList()
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Parameters {}

    protected Map<String, Object> maskPasswordsInConfiguration(Map<String, Object> configuration, ConfigurationRequest configurationRequest) {
        if (configuration == null || configurationRequest == null) {
            return configuration;
        }
        return configuration.entrySet()
                .stream()
                .collect(
                        HashMap::new,
                        (map, entry) -> {
                            final ConfigurationField field = configurationRequest.getField(entry.getKey());
                            if (field instanceof TextField && entry.getValue() instanceof String s && !Strings.isNullOrEmpty(s)) {
                                if (isPassword(field)) {
                                    map.put(entry.getKey(), "<password set>");
                                    return;
                                }
                                if (isEncrypted(field)) {
                                    map.put(entry.getKey(), "<value hidden>");
                                    return;
                                }
                            }
                            map.put(entry.getKey(), entry.getValue());
                        },
                        HashMap::putAll
                );
    }

    private static boolean isPassword(ConfigurationField field) {
        return field.getAttributes().contains(TextField.Attribute.IS_PASSWORD.toString().toLowerCase(Locale.ENGLISH));
    }

    private static boolean isEncrypted(ConfigurationField field) {
        return field.getAttributes().contains(TextField.Attribute.IS_SENSITIVE.toString().toLowerCase(Locale.ENGLISH));
    }
}
