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
package org.graylog2.system.urlwhitelist;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import static com.fasterxml.jackson.annotation.JsonSubTypes.Type;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({@Type(value = LiteralWhitelistEntry.class, name = "literal"),
        @Type(value = RegexWhitelistEntry.class, name = "regex")})
public interface WhitelistEntry {
    enum Type {
        @JsonProperty("literal")
        LITERAL,
        @JsonProperty("regex")
        REGEX
    }

    @JsonProperty("id")
    String id();

    @JsonProperty("type")
    Type type();

    @JsonProperty("title")
    String title();

    @JsonProperty("value")
    String value();

    boolean isWhitelisted(String url);
}
