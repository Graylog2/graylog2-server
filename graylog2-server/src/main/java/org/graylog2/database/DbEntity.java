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
package org.graylog2.database;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a class as a database entity.
 * <p>
 * <strong>Bindings need to be added explicitly with {@code org.graylog2.plugin.PluginModule#addDbEntities}</strong>.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(DbEntities.class)
public @interface DbEntity {
    String collection();

    String titleField() default "title";

    String readPermission() default NOBODY_ALLOWED;

    /**
     * List of document field names that are permitted to be read through general-purpose services
     * (e.g. {@code MongoEntitySuggestionService}) when the caller has the appropriate {@link #readPermission()}.
     * <p>
     * Only fields that are safe to expose should be listed here — sensitive data such as passwords,
     * tokens, or internal configuration objects must be excluded.
     */
    String[] readableFields() default {};

    //use for DBEntities that do not have string representations/ titles at all
    String NO_TITLE = "";

    String ALL_ALLOWED = "";
    String NOBODY_ALLOWED = "__no_access__";
}
