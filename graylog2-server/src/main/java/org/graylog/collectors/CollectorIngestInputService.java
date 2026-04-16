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

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.apache.shiro.subject.Subject;
import org.graylog.collectors.input.CollectorIngestHttpInput;
import org.graylog2.Configuration;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.inputs.transports.NettyTransport;
import org.graylog2.rest.models.system.inputs.requests.InputCreateRequest;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.graylog2.shared.inputs.NoSuchInputTypeException;
import org.graylog2.shared.security.RestPermissions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.graylog2.shared.utilities.StringUtils.f;

public class CollectorIngestInputService {
    private final InputService inputService;
    private final MessageInputFactory messageInputFactory;
    private final Configuration configuration;
    private final AuditEventSender auditEventSender;

    @Inject
    public CollectorIngestInputService(InputService inputService,
                                       MessageInputFactory messageInputFactory,
                                       Configuration configuration,
                                       AuditEventSender auditEventSender) {
        this.inputService = inputService;
        this.messageInputFactory = messageInputFactory;
        this.configuration = configuration;
        this.auditEventSender = auditEventSender;
    }

    public List<String> getInputIds() {
        return inputService.allByType(CollectorIngestHttpInput.class.getCanonicalName())
                .stream()
                .map(Input::getId)
                .toList();
    }

    public void createInput(Subject subject, String userName, int port) throws ValidationException {
        if (configuration.isCloud()) {
            throw new BadRequestException("Creating collector ingest inputs is not supported in cloud environments");
        }

        if (!subject.isPermitted(RestPermissions.INPUTS_CREATE)) {
            throw new ForbiddenException("Not permitted to create inputs");
        }
        if (!subject.isPermitted(f("%s:%s", RestPermissions.INPUT_TYPES_CREATE, CollectorIngestHttpInput.class.getCanonicalName()))) {
            throw new ForbiddenException(f("Not permitted to create input type %s", CollectorIngestHttpInput.class.getCanonicalName()));
        }

        final var inputType = CollectorIngestHttpInput.class.getCanonicalName();
        final var inputDescription = messageInputFactory.getAvailableInputs().get(inputType);
        if (inputDescription == null) {
            throw new NotFoundException(f("No input of type %s registered", inputType));
        }

        // Start with the default configuration values for the input type and then only override the port
        final var inputConfig = inputDescription.getConfigurationRequest().getFields().entrySet().stream()
                .filter(entry -> entry.getValue().getDefaultValue() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().getDefaultValue(),
                        (a, b) -> b,
                        HashMap::new
                ));
        inputConfig.put(NettyTransport.CK_PORT, port);

        try {
            final var inputCreateRequest = InputCreateRequest.create(
                    CollectorIngestHttpInput.NAME,
                    CollectorIngestHttpInput.class.getCanonicalName(),
                    true,
                    inputConfig,
                    null
            );
            final var messageInput = messageInputFactory.create(
                    inputCreateRequest, userName, inputCreateRequest.node(), false);
            messageInput.checkConfiguration();
            final var input = inputService.create(messageInput.asMap());
            inputService.save(input);
            auditEventSender.success(
                    AuditActor.user(userName),
                    AuditEventTypes.MESSAGE_INPUT_CREATE,
                    Map.of("request_entity", inputCreateRequest)
            );
        } catch (NoSuchInputTypeException e) {
            throw new NotFoundException("No such input type registered", e);
        } catch (ConfigurationException e) {
            throw new BadRequestException("Invalid input configuration", e);
        }
    }
}
