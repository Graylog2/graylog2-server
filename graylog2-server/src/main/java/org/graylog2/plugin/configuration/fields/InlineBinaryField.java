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

/**
 * Container for base64 encoded binary data. Could be rendered as a file selection tool which then reads the binary
 * content of the file and sets the field value to the base64 encoded representation of the byte array.
 */
public class InlineBinaryField extends AbstractConfigurationField {
    public static final String FIELD_TYPE = "inline_binary";

    private String defaultValue;

    /**
     * <em> Please make sure that the UI is supporting this field appropriately. The initial UI implementation
     * only handles fields with {@code encrypted = true}. This comment should be removed, once the UI implementation
     * is complete. </em>
     */
    public InlineBinaryField(String name, String humanName, String description,
                             Optional isOptional, boolean isEncrypted) {
        super(FIELD_TYPE, name, humanName, description, isOptional, DEFAULT_POSITION, isEncrypted);
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public void setDefaultValue(Object defaultValue) {
        if (defaultValue instanceof String str) {
            this.defaultValue = str;
        }
    }
}
