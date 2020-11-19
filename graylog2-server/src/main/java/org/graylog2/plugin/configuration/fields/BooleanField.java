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

public class BooleanField extends AbstractConfigurationField {

    public static final String FIELD_TYPE = "boolean";

    private boolean defaultValue;

    public BooleanField(String name, String humanName, boolean defaultValue, String description) {
        super(FIELD_TYPE, name, humanName, description, Optional.OPTIONAL);
        this.defaultValue = defaultValue;
    }
    public BooleanField(String name, String humanName, boolean defaultValue, String description, int position) {
        super(FIELD_TYPE, name, humanName, description, Optional.OPTIONAL, position);
        this.defaultValue = defaultValue;
    }

    @Override
    public Object getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public void setDefaultValue(Object defaultValue) {
        if (defaultValue instanceof Boolean) {
            this.defaultValue = (boolean) defaultValue;
        }
    }
}
