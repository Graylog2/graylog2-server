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
package org.graylog2.shared.inputs;

import jakarta.inject.Inject;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Startup-time sanity check for registered input types. Logs an ERROR for every
 * {@link TextField} declared with {@link TextField.Attribute#IS_PASSWORD} but not
 * encrypted at rest — the UI would mask the value as a password while MongoDB
 * persists it in plain text. Plugin authors should pair {@code IS_PASSWORD} with
 * {@code isEncrypted=true}, or drop {@code IS_PASSWORD} if the field is not a
 * credential.
 */
public class InputFieldEncryptionValidator {
    private static final Logger LOG = LoggerFactory.getLogger(InputFieldEncryptionValidator.class);
    private static final String IS_PASSWORD_ATTR = TextField.Attribute.IS_PASSWORD.toString().toLowerCase(Locale.ENGLISH);

    @Inject
    public InputFieldEncryptionValidator(MessageInputFactory messageInputFactory) {
        final List<String> offenders = findOffenders(messageInputFactory);
        if (!offenders.isEmpty()) {
            LOG.warn("""
                    WARNING! — Plain-text secrets at rest:
                    Input field(s) have IS_PASSWORD set without isEncrypted=true, so they are masked in the UI but stored as plain text.
                    Affected: {}""", String.join(", ", offenders));
        }
    }

    /**
     * Returns a list of "type-name field \"field-name\"" strings for every input field that has
     * {@code IS_PASSWORD} set but is not encrypted at rest. Exposed for testing.
     */
    static List<String> findOffenders(MessageInputFactory messageInputFactory) {
        final List<String> offenders = new ArrayList<>();
        messageInputFactory.getAvailableInputs().keySet().forEach(type ->
                messageInputFactory.getConfig(type).ifPresent(config -> {
                    try {
                        config.combinedRequestedConfiguration().getFields().values().stream()
                                .filter(InputFieldEncryptionValidator::isPasswordWithoutEncryption)
                                .forEach(f -> offenders.add(type + " field \"" + f.getName() + "\""));
                    } catch (Exception e) {
                        LOG.warn("Unable to validate TextField encryption on input type [{}].", type, e);
                    }
                }));
        return offenders;
    }

    private static boolean isPasswordWithoutEncryption(ConfigurationField field) {
        return !field.isEncrypted() && field.getAttributes().contains(IS_PASSWORD_ATTR);
    }
}
