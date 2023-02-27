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
package org.graylog2.inputs.encryption;

import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.security.encryption.EncryptedValue;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility functions to deal with input configuration that contains encrypted values.
 */
public class EncryptedInputConfigs {

    private EncryptedInputConfigs() {
    }

    /**
     * Merges two configuration by applying updates while treating encrypted values correctly
     *
     * @param orig   The original config that should be updated
     * @param update The new config containing the changes that should be applied to the original config
     * @return The merged config with encrypted values being properly handled
     */
    public static Map<String, Object> merge(Map<String, Object> orig, Map<String, Object> update) {
        final Map<String, Object> merged = new HashMap<>(orig);
        update.forEach((k, v) -> {
            if (orig.get(k) instanceof EncryptedValue origValue && v instanceof EncryptedValue newValue) {
                merged.put(k, mergeEncryptedValues(origValue, newValue));
            } else {
                merged.put(k, v);
            }
        });
        return merged;
    }

    /**
     * Returns the names of those fields in an input configuration that are expected to hold {@link EncryptedValue}s.
     */
    public static Set<String> getEncryptedFields(@Nonnull MessageInput.Config messageInputConfig) {
        return messageInputConfig.combinedRequestedConfiguration()
                .getFields()
                .values()
                .stream()
                .filter(ConfigurationField::isEncrypted)
                .map(ConfigurationField::getName)
                .collect(Collectors.toSet());
    }

    private static EncryptedValue mergeEncryptedValues(EncryptedValue origValue, EncryptedValue newValue) {
        if (newValue.isKeepValue()) {
            return origValue;
        }
        if (newValue.isDeleteValue()) {
            return EncryptedValue.createUnset();
        }
        return newValue;
    }
}
