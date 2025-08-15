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
package org.graylog2.users;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PasswordComplexityConfig(
        @JsonProperty("min_length") int minLength,
        @JsonProperty("require_uppercase") boolean requireUppercase,
        @JsonProperty("require_lowercase") boolean requireLowercase,
        @JsonProperty("require_numbers") boolean requireNumbers,
        @JsonProperty("require_special_chars") boolean requireSpecialCharacters
) {
    public static final String VALID_SPECIAL_CHARACTERS = "!@#$%^&*()-_=+[]{}|;:,.<>?/";
    public static final PasswordComplexityConfig DEFAULT =
            new PasswordComplexityConfig(6, false, false, false, false);
}
