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
package org.graylog.datanode.docs;

import jakarta.annotation.Nullable;

/**
 * @param configurationBean Class that defines this configuration entry
 * @param fieldName java field name
 * @param type Java type name, e.g. String or Integer.
 * @param configName configuration property name, as written in the config file
 * @param defaultValue default value declared in the java field, null if not defined
 * @param required if the configuration property is mandatory (needs default or entry in the config file)
 * @param documentation textual documentation of this configuration propery
 */
public record ConfigurationEntry(
        Class<?> configurationBean,
        String fieldName,
        String type,
        String configName,
        @Nullable Object defaultValue,
        boolean required,
        String documentation
) {

    public boolean isPriority() {
        return required && defaultValue == null;
    }
}
