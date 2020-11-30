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
package org.graylog2.rest.models.configuration.responses;

import java.util.Map;

public class DropdownField extends RequestedConfigurationField {
    private final static String TYPE = "dropdown";

    private final Map<String, String> values;

    public DropdownField(Map.Entry<String, Map<String, Object>> c) {
        super(TYPE, c);

        // lolwut
        this.values = (Map<String, String>) ((Map<String, Object>) c.getValue().get("additional_info")).get("values");
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String attributeToJSValidation(String attribute) {
        throw new RuntimeException("This type does not have any validatable attributes.");
    }

    public Map<String, String> getValues() {
        return values;
    }
}
