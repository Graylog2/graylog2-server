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
package org.graylog2.web.customization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Test
    void deserializesDisabledOnboardingResources() throws Exception {
        final var config = objectMapper.readValue("""
                {"onboarding": {"resources": {"enabled": false}}}
                """, Config.class);

        assertThat(config.onboarding()
                .flatMap(Config.Onboarding::resources)
                .flatMap(Config.Onboarding.OnboardingItem::enabled))
                .contains(false);
    }

    @Test
    void leavesOnboardingAbsentWhenNotConfigured() throws Exception {
        final var config = objectMapper.readValue("""
                {"product_name": "AwesomeLog"}
                """, Config.class);

        assertThat(config.onboarding()).isEmpty();
    }
}
