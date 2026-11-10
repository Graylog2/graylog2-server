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
import jakarta.validation.constraints.Min;
import org.graylog2.configuration.tunables.RuntimeTunable;
import org.graylog2.configuration.tunables.TunableField;

import java.util.Set;
import java.util.stream.Collectors;

@RuntimeTunable(id = "users.password_complexity", feature = "password_policy", title = "Password Complexity",
        description = "Configure the minimum password length and character requirements enforced for local users.")
public record PasswordComplexityConfig(
        @JsonProperty("min_length")
        @TunableField(label = "Minimum length", help = "Passwords shorter than this length will be rejected.")
        @Min(1) int minLength,
        @JsonProperty("require_uppercase")
        @TunableField(label = "Require uppercase letters", help = "Password must include at least one uppercase character (A-Z).")
        boolean requireUppercase,
        @JsonProperty("require_lowercase")
        @TunableField(label = "Require lowercase letters", help = "Password must include at least one lowercase character (a-z).")
        boolean requireLowercase,
        @JsonProperty("require_numbers")
        @TunableField(label = "Require numbers", help = "Password must include at least one numeric character (0-9).")
        boolean requireNumbers,
        @JsonProperty("require_special_chars")
        @TunableField(label = "Require special characters",
                help = "Password must include at least one of the following characters: " + PasswordComplexityConfig.SPECIAL_CHARACTERS)
        boolean requireSpecialCharacters
) {
    public static final String SPECIAL_CHARACTERS = "!@#$%^&*()-_=+[]{}|;:,.<>?/";
    public static final Set<Integer> SPECIAL_CHARACTERS_CODEPOINTS = SPECIAL_CHARACTERS.chars().boxed().collect(Collectors.toSet());
    public static final PasswordComplexityConfig DEFAULT =
            new PasswordComplexityConfig(6, false, false, false, false);

    public static PasswordComplexityConfig defaults() {
        return DEFAULT;
    }
}
