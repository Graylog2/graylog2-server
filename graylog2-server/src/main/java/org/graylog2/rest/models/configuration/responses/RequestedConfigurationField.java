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

import java.util.List;
import java.util.Locale;
import java.util.Map;

public abstract class RequestedConfigurationField {

    private final String title;
    private final String humanName;
    private final Object defaultValue;
    private final String description;
    private final boolean isOptional;
    private final List<String> attributes;

    public RequestedConfigurationField(String superType, Map.Entry<String, Map<String, Object>> c) {
        this.title = c.getKey();
        Map<String, Object> info = c.getValue();

        if (!info.get("type").equals(superType)) {
            throw new RuntimeException("Type does not match supertype. This should never happen.");
        }

        this.humanName = (String) info.get("human_name");
        this.defaultValue = info.get("default_value");
        this.description = (String) info.get("description");
        this.isOptional = (Boolean) info.get("is_optional");
        this.attributes = (List<String>) info.get("attributes");
    }

    public String getTitle() {
        return title;
    }

    public String getHumanName() {
        return humanName;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public String getDescription() {
        return description;
    }

    public boolean isOptional() {
        return isOptional;
    }

    public List<String> getAttributes() {
        return attributes;
    }

    public boolean hasAttribute(String attribute) {
        return attributes.contains(attribute.toLowerCase(Locale.ENGLISH));
    }

    public String getAttributesAsJSValidationSpec() {
        StringBuilder sb = new StringBuilder();

        int i = 0;
        for (String attribute : attributes) {
            if (i > 0) {
                sb.append(" ");
            }

            sb.append(attributeToJSValidation(attribute));
            i++;
        }

        return sb.toString();
    }

    public abstract String getType();
    public abstract String attributeToJSValidation(String attribute);
}
