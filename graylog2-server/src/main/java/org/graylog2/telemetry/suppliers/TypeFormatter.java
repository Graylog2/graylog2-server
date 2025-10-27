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
package org.graylog2.telemetry.suppliers;

import java.util.List;
import java.util.Locale;

public class TypeFormatter {
    private static final List<String> PACKAGE_PREFIXES = List.of(
            "org.graylog2.inputs",
            "org.graylog.plugins",
            "org.graylog.aws.inputs",
            "org.graylog.enterprise.integrations"
    );

    public static String format(String type) {
        for (String prefix : PACKAGE_PREFIXES) {
            if (type.startsWith(prefix + ".")) {
                return type.substring(type.lastIndexOf('.') + 1)
                        .replaceAll("([a-z\\d])([A-Z])", "$1_$2")
                        .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
                        .replaceAll("([A-Za-z])(\\d)", "$1_$2")
                        .replaceAll("(\\d)([A-Za-z])", "$1_$2")
                        .toLowerCase(Locale.ENGLISH);
            }
        }
        return type;
    }
}
