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
package org.graylog.integrations.dataadapters;

import org.graylog2.plugin.lookup.LookupDataAdapterConfiguration;
import org.graylog2.security.encryption.EncryptedValue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GreyNoiseQuickIPDataAdapterConfigTest {

    private static final EncryptedValue EXISTING_TOKEN = EncryptedValue.builder()
            .value("encrypted-secret")
            .salt("test-salt")
            .isKeepValue(false)
            .isDeleteValue(false)
            .build();

    private GreyNoiseQuickIPDataAdapter.Config existingConfig() {
        return GreyNoiseQuickIPDataAdapter.Config.builder()
                .type(GreyNoiseQuickIPDataAdapter.NAME)
                .apiToken(EXISTING_TOKEN)
                .build();
    }

    @Test
    void prepareConfigUpdatePreservesTokenWhenKeepValue() {
        final GreyNoiseQuickIPDataAdapter.Config existing = existingConfig();
        final GreyNoiseQuickIPDataAdapter.Config incoming = GreyNoiseQuickIPDataAdapter.Config.builder()
                .type(GreyNoiseQuickIPDataAdapter.NAME)
                .apiToken(EncryptedValue.createWithKeepValue())
                .build();

        final LookupDataAdapterConfiguration result = existing.prepareConfigUpdate(incoming);

        assertThat(result).isInstanceOf(GreyNoiseQuickIPDataAdapter.Config.class);
        final GreyNoiseQuickIPDataAdapter.Config resultConfig = (GreyNoiseQuickIPDataAdapter.Config) result;
        assertThat(resultConfig.apiToken()).isEqualTo(EXISTING_TOKEN);
        assertThat(resultConfig.apiToken().isSet()).isTrue();
    }

    @Test
    void prepareConfigUpdateClearsTokenWhenDeleteValue() {
        final GreyNoiseQuickIPDataAdapter.Config existing = existingConfig();
        final GreyNoiseQuickIPDataAdapter.Config incoming = GreyNoiseQuickIPDataAdapter.Config.builder()
                .type(GreyNoiseQuickIPDataAdapter.NAME)
                .apiToken(EncryptedValue.createWithDeleteValue())
                .build();

        final LookupDataAdapterConfiguration result = existing.prepareConfigUpdate(incoming);

        final GreyNoiseQuickIPDataAdapter.Config resultConfig = (GreyNoiseQuickIPDataAdapter.Config) result;
        assertThat(resultConfig.apiToken().isSet()).isFalse();
        assertThat(resultConfig.apiToken().isKeepValue()).isFalse();
        assertThat(resultConfig.apiToken().isDeleteValue()).isFalse();
    }

    @Test
    void prepareConfigUpdateUsesNewTokenWhenSetValue() {
        final GreyNoiseQuickIPDataAdapter.Config existing = existingConfig();
        final EncryptedValue newToken = EncryptedValue.builder()
                .value("new-encrypted-value")
                .salt("new-salt")
                .isKeepValue(false)
                .isDeleteValue(false)
                .build();
        final GreyNoiseQuickIPDataAdapter.Config incoming = GreyNoiseQuickIPDataAdapter.Config.builder()
                .type(GreyNoiseQuickIPDataAdapter.NAME)
                .apiToken(newToken)
                .build();

        final LookupDataAdapterConfiguration result = existing.prepareConfigUpdate(incoming);

        final GreyNoiseQuickIPDataAdapter.Config resultConfig = (GreyNoiseQuickIPDataAdapter.Config) result;
        assertThat(resultConfig.apiToken()).isEqualTo(newToken);
    }

    @Test
    void defaultPrepareConfigUpdateReturnsNewConfig() {
        // Verify the default interface method just passes through
        final GreyNoiseQuickIPDataAdapter.Config config = existingConfig();
        final GreyNoiseQuickIPDataAdapter.Config incoming = GreyNoiseQuickIPDataAdapter.Config.builder()
                .type(GreyNoiseQuickIPDataAdapter.NAME)
                .apiToken(EncryptedValue.createUnset())
                .build();

        // The override should still work, but let's verify the result is the incoming config when a new value is set
        final LookupDataAdapterConfiguration result = config.prepareConfigUpdate(incoming);
        final GreyNoiseQuickIPDataAdapter.Config resultConfig = (GreyNoiseQuickIPDataAdapter.Config) result;
        assertThat(resultConfig.apiToken()).isEqualTo(EncryptedValue.createUnset());
    }
}
