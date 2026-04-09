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
package org.graylog.collectors;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.ws.rs.InternalServerErrorException;
import org.graylog.collectors.rest.CollectorsConfigRequest;
import org.graylog2.database.NotFoundException;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.inputs.MessageInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.graylog2.shared.utilities.StringUtils.f;

public class CollectorInputService {
    private static final Logger LOG = LoggerFactory.getLogger(CollectorInputService.class);

    private final InputService inputService;

    @Inject
    public CollectorInputService(InputService inputService) {
        this.inputService = inputService;
    }

    @Nullable
    public String reconcile(CollectorsConfigRequest.IngestEndpointRequest requested,
                            @Nullable IngestEndpointConfig existing,
                            String inputType,
                            String title,
                            String creatorUserId) {
        final String existingInputId = existing != null ? existing.inputId() : null;
        final boolean inputExists = existingInputId != null && inputExistsInDb(existingInputId);

        if (requested.enabled()) {
            if (!inputExists) {
                return createManagedInput(inputType, title, creatorUserId);
            }
            if (existing.port() != requested.port()) {
                restartInput(existingInputId);
            }
            return existingInputId;
        } else {
            if (inputExists) {
                deleteManagedInput(existingInputId);
            }
            return null;
        }
    }

    private boolean inputExistsInDb(String inputId) {
        try {
            inputService.find(inputId);
            return true;
        } catch (NotFoundException e) {
            return false;
        }
    }

    private String createManagedInput(String inputType, String title, String creatorUserId) {
        final var input = inputService.create(Map.of(
                MessageInput.FIELD_TYPE, inputType,
                MessageInput.FIELD_TITLE, title,
                MessageInput.FIELD_CREATOR_USER_ID, creatorUserId,
                MessageInput.FIELD_GLOBAL, true,
                MessageInput.FIELD_CONFIGURATION, Map.of(),
                MessageInput.FIELD_DESIRED_STATE, IOState.Type.RUNNING.name()
        ));
        try {
            return inputService.save(input);
        } catch (ValidationException e) {
            throw new InternalServerErrorException(f("Failed to create managed input: %s", title), e);
        }
    }

    private void restartInput(String inputId) {
        try {
            final Input input = inputService.find(inputId);
            inputService.update(input);
        } catch (NotFoundException e) {
            LOG.warn("Input {} not found during restart attempt", inputId);
        } catch (ValidationException e) {
            throw new InternalServerErrorException(f("Failed to restart input %s", inputId), e);
        }
    }

    private void deleteManagedInput(String inputId) {
        try {
            final Input input = inputService.find(inputId);
            inputService.destroy(input);
        } catch (NotFoundException e) {
            LOG.warn("Input {} not found during delete attempt", inputId);
        }
    }
}
