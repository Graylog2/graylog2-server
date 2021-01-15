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

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Locale;

public class NumberField extends AbstractConfigurationField {

    public static final String FIELD_TYPE = "number";

    public enum Attribute {
        ONLY_POSITIVE,
        ONLY_NEGATIVE,
        IS_PORT_NUMBER
    }

    private Number defaultValue;

    private final List<String> attributes;

    public NumberField(String name, String humanName, int defaultValue, String description, Optional isOptional) {
        this(name, humanName, defaultValue, description, isOptional, new Attribute[0]);
    }

    public NumberField(String name, String humanName, double defaultValue, String description, Optional isOptional) {
        this(name, humanName, defaultValue, description, isOptional, new Attribute[0]);
    }

    public NumberField(String name, String humanName, int defaultValue, String description, Attribute... attributes) {
        this(name, humanName, defaultValue, description, Optional.NOT_OPTIONAL, attributes);
    }

    public NumberField(String name, String humanName, double defaultValue, String description, Attribute... attributes) {
        this(name, humanName, defaultValue, description, Optional.NOT_OPTIONAL, attributes);
    }

    public NumberField(String name, String humanName, int defaultValue, String description, Optional isOptional, Attribute... attributes) {
        this(name, humanName, (Number) defaultValue, description, isOptional, ConfigurationField.DEFAULT_POSITION, attributes);
    }

    public NumberField(String name, String humanName, double defaultValue, String description, Optional isOptional, Attribute... attributes) {
        this(name, humanName, (Number) defaultValue, description, isOptional, ConfigurationField.DEFAULT_POSITION, attributes);
    }

    public NumberField(String name, String humanName, int defaultValue, String description, Optional isOptional, int position, Attribute... attributes) {
        this(name, humanName, (Number) defaultValue, description, isOptional, position, attributes);
    }

    public NumberField(String name, String humanName, double defaultValue, String description, Optional isOptional, int position, Attribute... attributes) {
        this(name, humanName, (Number) defaultValue, description, isOptional, position, attributes);
    }

    private NumberField(String name, String humanName, Number defaultValue, String description, Optional isOptional, int position, Attribute... attributes) {
        super(FIELD_TYPE, name, humanName, description, isOptional);
        this.defaultValue = defaultValue;
        this.position = position;

        this.attributes = Lists.newArrayList();
        if (attributes != null) {
            for (Attribute attribute : attributes) {
                this.attributes.add(attribute.toString().toLowerCase(Locale.ENGLISH));
            }
        }
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

    @Override
    public void setDefaultValue(Object defaultValue) {
        if (defaultValue instanceof Number) {
            this.defaultValue = (Number) defaultValue;
        }
    }

    @Override
    public List<String> getAttributes() {
        return attributes;
    }
}
