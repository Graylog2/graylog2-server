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

import org.graylog2.security.encryption.EncryptedValue;

import java.util.HashMap;
import java.util.Map;

public class EncryptedInputConfigs {

    private EncryptedInputConfigs() {
    }

    public static Map<String, Object> mergeInputConfiguration(Map<String, Object> orig, Map<String, Object> update) {
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
