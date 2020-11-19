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

import java.util.Locale;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class NumberField extends RequestedConfigurationField {

    private final static String TYPE = "number";

    public enum Attribute {
        ONLY_POSITIVE,
        ONLY_NEGATIVE,
        IS_PORT_NUMBER
    }

    public NumberField(Map.Entry<String, Map<String, Object>> c) {
        super(TYPE, c);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String attributeToJSValidation(String attribute) {
        switch (Attribute.valueOf(attribute.toUpperCase(Locale.ENGLISH))) {
            case ONLY_NEGATIVE:
                return "negative_number";
            case ONLY_POSITIVE:
                return "positive_number";
            case IS_PORT_NUMBER:
                return "port_number";
            default:
                throw new RuntimeException("No JS validation for type [" + attribute + "].");
        }
    }

}
