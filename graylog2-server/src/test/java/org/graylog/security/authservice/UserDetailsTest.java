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
package org.graylog.security.authservice;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class UserDetailsTest {

    @Test
    public void testNameBuildValidations() {

        // No firstName, lastName, or fullName
        assertThatThrownBy(() -> baseBuilder().build())
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Either a firstName/lastName or a fullName are required");

        // Missing lastName
        assertThatThrownBy(() -> baseBuilder().firstName("First").build())
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Either a firstName/lastName or a fullName are required");

        // Missing firstName
        assertThatThrownBy(() -> baseBuilder().firstName("First").build())
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Either a firstName/lastName or a fullName are required");

        // Has fullName, build should succeed.
        baseBuilder().fullName("Full Name").build();

        // Has firstName and lastName, build should succeed.
        baseBuilder().firstName("First").lastName("Last").build();
    }

    private UserDetails.Builder baseBuilder() {
        return UserDetails.builder()
                          .authServiceType("auth-type")
                          .authServiceId("auth-id")
                          .base64AuthServiceUid(new String(Base64.getEncoder()
                                                                 .encode("uid".getBytes(StandardCharsets.UTF_8))))
                          .username("username")
                          .accountIsEnabled(true)
                          .email("email@graylog.com")
                          .defaultRoles(new HashSet<>());
    }
}
