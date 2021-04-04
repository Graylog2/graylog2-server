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

import java.util.Map;

public class RadioField extends AbstractChoiceField {

    public static final String FIELD_TYPE = "radio";

    public RadioField(String name, String humanName, String defaultValue, Map<String, String> values, Optional isOptional) {
        super(FIELD_TYPE, name, humanName, defaultValue, values, isOptional);
    }

    public RadioField(String name, String humanName, String defaultValue, Map<String, String> values, String description, Optional isOptional) {
        super(FIELD_TYPE, name, humanName, defaultValue, values, description, isOptional);
    }

    public RadioField(String name, String humanName, String defaultValue, Map<String, String> values, String description, Optional isOptional, int position) {
        super(FIELD_TYPE, name, humanName, defaultValue, values, description, isOptional, position);
    }
}
