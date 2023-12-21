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
package org.graylog2.streams.input;

import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog2.shared.inputs.InputDescription;
import org.graylog2.shared.inputs.MessageInputFactory;

import jakarta.inject.Inject;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class StreamRuleServerInputsProvider implements StreamRuleInputsProvider {

    private final InputService inputService;
    private final Map<String, InputDescription> inputDescriptions;

    @Inject
    public StreamRuleServerInputsProvider(InputService inputService, MessageInputFactory messageInputFactory) {
        this.inputService = inputService;
        inputDescriptions = messageInputFactory.getAvailableInputs();
    }

    @Override
    public Set<StreamRuleInput> inputs() {
        return inputService.all().stream()
                .map(this::toStreamRuleInput)
                .collect(Collectors.toSet());
    }

    private StreamRuleInput toStreamRuleInput(Input input) {
        return StreamRuleInput.builder()
                .title(input.getTitle())
                .name(getInputName(input))
                .id(input.getId())
                .build();
    }

    private String getInputName(Input input) {
        return InputDescription.getInputDescriptionName(inputDescriptions.get(input.getType()), input.getType());
    }

}
