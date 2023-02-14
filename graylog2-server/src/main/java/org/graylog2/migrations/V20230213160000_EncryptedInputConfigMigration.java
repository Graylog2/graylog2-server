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
package org.graylog2.migrations;

import org.graylog2.inputs.EncryptedInputConfigs;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.Set;

public class V20230213160000_EncryptedInputConfigMigration extends Migration {

    private static final Logger LOG = LoggerFactory.getLogger(V20230213160000_EncryptedInputConfigMigration.class);

    private final InputService inputService;
    private final MessageInputFactory messageInputFactory;

    @Inject
    public V20230213160000_EncryptedInputConfigMigration(InputService inputService,
                                                         MessageInputFactory messageInputFactory) {
        this.inputService = inputService;
        this.messageInputFactory = messageInputFactory;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2023-02-13T16:00:00Z");
    }

    @Override
    public void upgrade() {
        // Migration is done implicitly in EncryptedValueDeserializer
        for (Input input : inputService.all()) {
            if (getEncryptedFields(input.getType()).isEmpty()) {
                continue;
            }
            try {
                inputService.saveWithoutEvents(input);
            } catch (ValidationException e) {
                LOG.warn("Encryption migration failed for input {} ({})!", input.getTitle(), input.getType());
            }
        }
    }

    private Set<String> getEncryptedFields(String type) {
        return messageInputFactory.getConfig(type)
                .map(EncryptedInputConfigs::getEncryptedFields)
                .orElse(Set.of());
    }
}
