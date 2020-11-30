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

import com.github.jcustenborder.cef.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MappedMessage implements Message {
    private static final Logger LOG = LoggerFactory.getLogger(MappedMessage.class);
    private final Message message;
    private static final String LABEL_SUFFIX = "Label";
    private final boolean useFullNames;
    private final Map<String, Object> extensions;

    public MappedMessage(Message message, boolean useFullNames) {
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
            if (fieldMapping != null) {
                try {
                    mappedExtensions.put(getLabel(keyName, fieldMapping.getFullName(), extensions), fieldMapping.convert(extension.getValue()));
                } catch (Exception e) {
                    LOG.warn("Could not transform CEF field [{}] according to standard. Skipping.", keyName, e);
                }
            } else {
                mappedExtensions.put(getLabel(keyName, keyName, extensions), extension.getValue());
            }

        }
        return mappedExtensions;
    }

    private String getLabel(String keyName, String fullName, Map<String, String> extensions) {
        final String labelName = keyName + LABEL_SUFFIX;
        return extensions.getOrDefault(labelName, useFullNames ? fullName : keyName);
    }

    @Override
    public Date timestamp() {
        return message.timestamp();
    }

    @Override
    public String host() {
        return message.host();
    }

    @Override
    public int cefVersion() {
        return message.cefVersion();
    }

    @Override
    public String deviceVendor() {
        return message.deviceVendor();
    }

    @Override
    public String deviceProduct() {
        return message.deviceProduct();
    }

    @Override
    public String deviceVersion() {
        return message.deviceVersion();
    }

    @Override
    public String deviceEventClassId() {
        return message.deviceEventClassId();
    }

    @Override
    public String name() {
        return message.name();
    }

    @Override
    public String severity() {
        return message.severity();
    }

    @Override
    public Map<String, String> extensions() {
        return message.extensions();
    }

    public Map<String, Object> mappedExtensions() {
        return extensions;
    }
}
