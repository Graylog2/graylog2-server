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
package org.graylog2.rest.models.tools.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.validation.constraints.NotEmpty;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class JsonTestRequest {
    @JsonProperty("flatten")
    public abstract boolean flatten();

    @JsonProperty("list_separator")
    @NotEmpty
    public abstract String listSeparator();

    @JsonProperty("key_separator")
    @NotEmpty
    public abstract String keySeparator();

    @JsonProperty("kv_separator")
    @NotEmpty
    public abstract String kvSeparator();

    @JsonProperty("replace_key_whitespace")
    public abstract boolean replaceKeyWhitespace();

    @JsonProperty("key_whitespace_replacement")
    public abstract String keyWhitespaceReplacement();

    @JsonProperty("key_prefix")
    public abstract String keyPrefix();

    @JsonProperty("string")
    @NotEmpty
    public abstract String string();

    @JsonCreator
    public static JsonTestRequest create(@JsonProperty("flatten") boolean flatten,
                                         @JsonProperty("list_separator") @NotEmpty String listSeparator,
                                         @JsonProperty("key_separator") @NotEmpty String keySeparator,
                                         @JsonProperty("kv_separator") @NotEmpty String kvSeparator,
                                         @JsonProperty("replace_key_whitespace") boolean replaceKeyWhitespace,
                                         @JsonProperty("key_whitespace_replacement") String keyWhitespaceReplacement,
                                         @JsonProperty("key_prefix") String keyPrefix,
                                         @JsonProperty("string") @NotEmpty String string) {
        return new AutoValue_JsonTestRequest(flatten, listSeparator, keySeparator, kvSeparator, replaceKeyWhitespace, keyWhitespaceReplacement, keyPrefix, string);
    }
}
