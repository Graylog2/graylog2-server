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
package org.graylog2.migrations;

import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.users.UserConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.threeten.extra.PeriodDuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class V20250804104500_TightenTokenSecurityTest {
    //We prepare some existing config with explicitly updated values, so we can safely check they're not touched by the migration:
    private final UserConfiguration existingConfig = UserConfiguration.create(true, Duration.of(10, ChronoUnit.HOURS), true, false, PeriodDuration.of(Duration.ofDays(7)));

    @Mock
    private ClusterConfigService configService;

    private V20250804104500_TightenTokenSecurity testee;

    @BeforeEach
    void setUp() {
        testee = new V20250804104500_TightenTokenSecurity(configService);
    }

    @Test
    void doNothingIfMigrationAlreadyRanSuccessfully() {
        setupMocks(true, false);

        testee.upgrade();

        verify(configService).get(V20250804104500_TightenTokenSecurity.MigrationCompleted.class);
        verifyNoMoreInteractions(configService);
    }

    @Test
    void persistDefaultValuesIfNoConfigExists() {
        testee = new V20250804104500_TightenTokenSecurity(configService);
        setupMocks(false, false);

        testee.upgrade();

        verify(configService).get(V20250804104500_TightenTokenSecurity.MigrationCompleted.class);
        verify(configService).write(UserConfiguration.DEFAULT_VALUES);
        verify(configService).write(new V20250804104500_TightenTokenSecurity.MigrationCompleted());
        verifyNoMoreInteractions(configService);
    }

    @Test
    void existingConfigIsUpdatedWithStricterValues() {
        setupMocks(false, true);
        //Expected to be written - keeps existing values for globalSessionTimeout and -interval, but applies default values for token access management
        final UserConfiguration updated = UserConfiguration.create(existingConfig.enableGlobalSessionTimeout(),
                existingConfig.globalSessionTimeoutInterval(),
                UserConfiguration.DEFAULT_VALUES.allowAccessTokenForExternalUsers(),
                UserConfiguration.DEFAULT_VALUES.restrictAccessTokenToAdmins(),
                UserConfiguration.DEFAULT_VALUES.defaultTTLForNewTokens());

        testee.upgrade();

        verify(configService).get(V20250804104500_TightenTokenSecurity.MigrationCompleted.class);
        verify(configService).get(UserConfiguration.class);
        verify(configService).write(updated);
        verify(configService).write(new V20250804104500_TightenTokenSecurity.MigrationCompleted());
        verifyNoMoreInteractions(configService);
    }


    private void setupMocks(boolean migrationAlreadyRan, boolean configExists) {
        if (migrationAlreadyRan) {
            when(configService.get(V20250804104500_TightenTokenSecurity.MigrationCompleted.class)).thenReturn(new V20250804104500_TightenTokenSecurity.MigrationCompleted());
        } else {
            when(configService.get(V20250804104500_TightenTokenSecurity.MigrationCompleted.class)).thenReturn(null);
            when(configService.get(UserConfiguration.class)).thenReturn(configExists ? existingConfig : null);
        }
    }
}
