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
package org.graylog2.configuration.tunables;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.temporal.ChronoUnit;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
public @interface TunableField {
    String label();

    String help() default "";

    int order() default 0;

    /** Smallest unit offered by the UI for duration fields. */
    ChronoUnit minUnit() default ChronoUnit.SECONDS;

    /** All conditions must match for the field to be editable. */
    When[] enabledWhen() default {};

    /** All conditions must match for the field to be shown. */
    When[] visibleWhen() default {};

    /** Restricts the values offered for an enum field. */
    String[] allowedValues() default {};

    /** Hides the field in the UI while keeping its value. */
    boolean hidden() default false;
}
