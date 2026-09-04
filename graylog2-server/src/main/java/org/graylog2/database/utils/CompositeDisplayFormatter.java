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
package org.graylog2.database.utils;

import org.bson.Document;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Formats a composite display value from multiple document fields.
 * <p>
 * If a template is provided, it replaces {@code {field}} placeholders with values from the document.
 * If no template is provided, it concatenates all field values with spaces.
 * Missing or null field values are handled gracefully.
 */
public class CompositeDisplayFormatter {

    private CompositeDisplayFormatter() {}

    public static String format(Document doc, List<String> fields, @Nullable String template) {
        if (template != null && !template.isEmpty()) {
            String result = template;
            for (String field : fields) {
                String value = doc.getString(field);
                if (value != null) {
                    result = result.replace("{" + field + "}", value);
                } else {
                    result = result.replace("{" + field + "}", "");
                }
            }
            return result.replaceAll("\\s*\\(\\s*\\)", "").replaceAll("\\s+", " ").trim();
        } else {
            return fields.stream()
                    .map(doc::getString)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(" "));
        }
    }
}
