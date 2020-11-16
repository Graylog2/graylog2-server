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
package org.graylog2.rest.resources.system.inputs;

import com.google.common.base.Strings;
import org.graylog2.inputs.Input;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.rest.models.system.inputs.responses.InputSummary;
import org.graylog2.shared.inputs.InputDescription;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Contains functionality that is used in multiple API resources.
 */
public class AbstractInputsResource extends RestResource {

    private final Map<String, InputDescription> availableInputs;

    public AbstractInputsResource(Map<String, InputDescription> availableInputs) {
        this.availableInputs = availableInputs;
    }

    /**
     * @return A {@link InputSummary} JSON value object for the input entity.
     */
    protected InputSummary getInputSummary(Input input) {
        final InputDescription inputDescription = this.availableInputs.get(input.getType());
        final String name = inputDescription != null ? inputDescription.getName() : "Unknown Input (" + input.getType() + ")";
        final ConfigurationRequest configurationRequest = inputDescription != null ? inputDescription.getConfigurationRequest() : null;
        final Map<String, Object> configuration = isPermitted(RestPermissions.INPUTS_EDIT, input.getId()) ?
                input.getConfiguration() : maskPasswordsInConfiguration(input.getConfiguration(), configurationRequest);
        return InputSummary.create(input.getTitle(),
                input.isGlobal(),
                name,
                input.getContentPack(),
                input.getId(),
                input.getCreatedAt(),
                input.getType(),
                input.getCreatorUserId(),
                configuration,
                input.getStaticFields(),
                input.getNodeId());
    }

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
                            if (field instanceof TextField) {
                                final TextField textField = (TextField) field;
                                if (textField.getAttributes().contains(TextField.Attribute.IS_PASSWORD.toString().toLowerCase(Locale.ENGLISH))
                                        && !Strings.isNullOrEmpty((String) entry.getValue())) {
                                    map.put(entry.getKey(), "<password set>");
                                    return;
                                }
                            }
                            map.put(entry.getKey(), entry.getValue());
                        },
                        HashMap::putAll
                );
    }
}
