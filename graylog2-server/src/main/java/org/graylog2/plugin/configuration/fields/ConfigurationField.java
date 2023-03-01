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
package org.graylog2.plugin.configuration.fields;

import java.util.List;
import java.util.Map;

public interface ConfigurationField {
    int DEFAULT_POSITION = 100;  // corresponds to ConfigurationForm.jsx
    int PLACE_AT_END_POSITION = 200;
    boolean DEFAULT_IS_ENCRYPTED = false;  // corresponds to ConfigurationForm.jsx

    enum Optional {
        OPTIONAL,
        NOT_OPTIONAL
    }

    String getFieldType();

    String getName();

    String getHumanName();

    String getDescription();

    Object getDefaultValue();

    void setDefaultValue(Object defaultValue);

    Optional isOptional();

    List<String> getAttributes();

    Map<String, Map<String, String>> getAdditionalInformation();

    default int getPosition() {
        return DEFAULT_POSITION;
    }

    default boolean isEncrypted() {
        return DEFAULT_IS_ENCRYPTED;
    }
}
