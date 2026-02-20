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
package org.graylog2.security.realm;

import org.apache.shiro.authc.SimpleAccount;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.security.realm.ServiceValidatedCredentialsMatcher.AUTHENTICATED;
import static org.graylog2.security.realm.ServiceValidatedCredentialsMatcher.SIMULATED;

class ServiceValidatedCredentialsMatcherTest {

    @Test
    void createSimulatedCredentials() {
        final var matcher = new ServiceValidatedCredentialsMatcher();

        assertThat(matcher.createSimulatedCredentials()).get()
                .satisfies(credentials -> assertThat(credentials.getCredentials()).isEqualTo(SIMULATED));
    }

    @Test
    void doCredentialsMatch() {
        final var matcher = new ServiceValidatedCredentialsMatcher();
        final var authenticated = new SimpleAccount("authenticated", AUTHENTICATED, "authenticated realm");
        final var simulated = new SimpleAccount("simulated", SIMULATED, "simulated realm");

        assertThat(matcher.doCredentialsMatch(null, authenticated)).isTrue();
        assertThat(matcher.doCredentialsMatch(null, simulated)).isFalse();
    }
}
