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
package org.graylog.plugins.cef.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MappedMessage {
    private static final Logger LOG = LoggerFactory.getLogger(MappedMessage.class);
    private final CEFMessage message;
    private static final String LABEL_SUFFIX = "Label";
    private final boolean useFullNames;
    private final Map<String, Object> extensions;

    public MappedMessage(CEFMessage message, boolean useFullNames) {
        this.message = message;
        this.useFullNames = useFullNames;
        this.extensions = mapExtensions(message.extensions());
    }

    private Map<String, Object> mapExtensions(Map<String, String> extensions) {
        final Map<String, Object> mappedExtensions = new HashMap<>();
        for (Map.Entry<String, String> extension : extensions.entrySet()) {
            final String keyName = extension.getKey();
            if (keyName.endsWith(LABEL_SUFFIX)) {
                LOG.trace("Skipping label: {}", keyName);
                continue;
            }

            final CEFMapping fieldMapping = CEFMapping.forKeyName(keyName);
            final String value = extension.getValue();
            if (fieldMapping != null) {
                try {
                    final Object converted = fieldMapping.convert(value);
                    if (converted == null) {
                        LOG.trace("CEF field [{}] has an empty value. Skipping.", keyName);
                    } else {
                        mappedExtensions.put(getLabel(keyName, fieldMapping.getFullName(), extensions), converted);
                    }
                } catch (Exception e) {
                    LOG.warn("Could not transform CEF field [{}] with value [{}] according to standard. Skipping.",
                            keyName, value);
                }
            } else {
                mappedExtensions.put(getLabel(keyName, keyName, extensions), value);
            }

        }
        return mappedExtensions;
    }

    private String getLabel(String keyName, String fullName, Map<String, String> extensions) {
        final String labelName = keyName + LABEL_SUFFIX;
        return extensions.getOrDefault(labelName, useFullNames ? fullName : keyName);
    }

    public Date timestamp() {
        return message.timestamp();
    }

    public String host() {
        return message.host();
    }

    public int cefVersion() {
        return message.cefVersion();
    }

    public String deviceVendor() {
        return message.deviceVendor();
    }

    public String deviceProduct() {
        return message.deviceProduct();
    }

    public String deviceVersion() {
        return message.deviceVersion();
    }

    public String deviceEventClassId() {
        return message.deviceEventClassId();
    }

    public String name() {
        return message.name();
    }

    public String severity() {
        return message.severity();
    }

    public Map<String, String> extensions() {
        return message.extensions();
    }

    public Map<String, Object> mappedExtensions() {
        return extensions;
    }
}
