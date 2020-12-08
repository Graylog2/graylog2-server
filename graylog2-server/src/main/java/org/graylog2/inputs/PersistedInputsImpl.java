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
package org.graylog2.inputs;

import com.google.common.collect.Lists;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.inputs.Extractor;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.shared.inputs.NoSuchInputTypeException;
import org.graylog2.shared.inputs.PersistedInputs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

public class PersistedInputsImpl implements PersistedInputs {
    private static final Logger LOG = LoggerFactory.getLogger(PersistedInputsImpl.class);
    private final InputService inputService;
    private final ServerStatus serverStatus;

    @Inject
    public PersistedInputsImpl(InputService inputService, ServerStatus serverStatus) {
        this.inputService = inputService;
        this.serverStatus = serverStatus;
    }

    @Override
    public Iterator<MessageInput> iterator() {
        List<MessageInput> result = Lists.newArrayList();

        for (Input io : inputService.allOfThisNode(serverStatus.getNodeId().toString())) {
            try {
                final MessageInput input = inputService.getMessageInput(io);
                result.add(input);
            } catch (NoSuchInputTypeException e) {
                LOG.warn("Cannot instantiate persisted input. No such type [{}].", io.getType());
            } catch (Throwable e) {
                LOG.warn("Cannot instantiate persisted input. Exception caught: ", e);
            }
        }

        return result.iterator();
    }

    @Override
    public MessageInput get(String id) {
        try {
            return inputService.getMessageInput(inputService.find(id));
        } catch (NoSuchInputTypeException e) {
            LOG.warn("Cannot instantiate persisted input: ", e);
        } catch (NotFoundException e) {
            LOG.warn("Cannot find persisted Input with id {}", id);
        }

        return null;
    }

    @Override
    public boolean add(MessageInput input) {
        try {
            final Input mongoInput = getInput(input);
            // Persist input.
            String id = inputService.save(mongoInput);

            input.setPersistId(id);
            return true;
        } catch (ValidationException e) {
            return false;
        }
    }

    @Override
    public boolean remove(Object o) {
        if (o instanceof MessageInput) {
            final MessageInput messageInput = (MessageInput) o;
            if (isNullOrEmpty(messageInput.getId()))
                return false;
            try {
                final Input input = inputService.find(messageInput.getId());
                inputService.destroy(input);
                return true;
            } catch (NotFoundException e) {
                return false;
            }
        }

        return false;
    }

    @Override
    public boolean update(String id, MessageInput newInput) {
        try {
            final Input oldInput = inputService.find(id);
            newInput.setPersistId(id);
            final Input mongoInput = getInput(newInput);

            final List<Extractor> extractors = inputService.getExtractors(oldInput);
            final Map<String, String> staticFields = oldInput.getStaticFields();

            inputService.save(mongoInput);

            for (Map.Entry<String, String> entry : staticFields.entrySet())
                inputService.addStaticField(mongoInput, entry.getKey(), entry.getValue());

            for (Extractor extractor : extractors)
                inputService.addExtractor(mongoInput, extractor);

            return true;
        } catch (NotFoundException | ValidationException e) {
            return false;
        }
    }

    private Input getInput(MessageInput input) throws ValidationException {
        // Build MongoDB data
        final Map<String, Object> inputData = input.asMap();

        // ... and check if it would pass validation. We don't need to go on if it doesn't.
        final Input mongoInput;
        if (input.getId() != null)
            mongoInput = inputService.create(input.getId(), inputData);
        else
            mongoInput = inputService.create(inputData);

        return mongoInput;
    }
}
