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
package org.graylog.jsonschema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;

/**
 * Lists the values a string property should have, as the names of the given enum's constants, without changing its
 * Java type, so stored values outside the list still load. {@link AllowedValuesModule} adds them to generated schemas.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface AllowedValues {
    Class<? extends Enum<?>> value();

    final class Values {
        private Values() {
        }

        public static List<String> of(AllowedValues allowedValues) {
            return Arrays.stream(allowedValues.value().getEnumConstants()).map(Enum::name).toList();
        }
    }
}
