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
package org.graylog2.jackson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that can be used to set a default logical type name for documents that are missing it.
 * This can be useful to set a default type for old JSON documents when migrating data structures from an
 * untyped to a typed version.
 * <p>
 * Example:
 * <pre>{@code
 *   @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
 *   @JsonSubTypes(@JsonSubTypes.Type(value = SubTypeA.class, name = "a"))
 *   @JsonSubTypePropertyDefaultValue("a")
 *   public @interface TestInterface {
 *   }
 * }</pre>
 * The difference to the {@code defaultImpl} parameter for {@link JsonTypeInfo} is,
 * that the default value is only used when the type attribute it's missing in a document. Using {@code defaultImpl}
 * will use the given default implementation in all cases, even if a document contains a non-existent type name.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonSubTypePropertyDefaultValue {
    String value();
}
