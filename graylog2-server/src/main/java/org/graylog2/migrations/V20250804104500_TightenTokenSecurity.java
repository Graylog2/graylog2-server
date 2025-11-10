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

import jakarta.inject.Inject;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.users.UserConfiguration;

import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Updates {@link UserConfiguration} in the DB to make the security for tokens stricter.
 *
 * A possibly existing {@link UserConfiguration} is updated to restrict creation of tokens to admins and local users only.
 * So no regular or externally authenticated user is allowed to create a token. Also, the default time-to-live (TTL) is
 * set to 30 days.
 */
public class V20250804104500_TightenTokenSecurity extends Migration {
    private final ClusterConfigService configService;

    //No breaking changes in minor versions (i.e. access must not be limited for upgrades).
    // When it comes to upgrading to 7.0, access should be restricted.

    @Inject
    public V20250804104500_TightenTokenSecurity(ClusterConfigService configService) {
        this.configService = configService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2025-08-04T10:45:00Z");
    }

    @Override
    public void upgrade() {
        if (migrationAlreadyApplied()) {
            //Migration already ran, nothing more to do.
            return;
        }

        //This migration comes with 7.0, so we're allowed to do breaking changes.
        final UserConfiguration newDefaults = UserConfiguration.DEFAULT_VALUES;

        UserConfiguration configToUpdate = this.configService.get(UserConfiguration.class);
        if (configToUpdate == null) {
            //No userConfig exists, let's simply save the default for the current version:
            configToUpdate = newDefaults;
        } else {
            //A UserConfig already exists. We tighten the security when it comes to who is allowed to create a token
            // and how long it will be active. The remaining settings stay unchanged:
            configToUpdate = UserConfiguration.create(
                    configToUpdate.enableGlobalSessionTimeout(),
                    configToUpdate.globalSessionTimeoutInterval(),
                    newDefaults.allowAccessTokenForExternalUsers(),
                    newDefaults.restrictAccessTokenToAdmins(),
                    newDefaults.defaultTTLForNewTokens());
        }

        configService.write(configToUpdate);

        markMigrationApplied();
    }

    private boolean migrationAlreadyApplied() {
        return Objects.nonNull(configService.get(V20250804104500_TightenTokenSecurity.MigrationCompleted.class));
    }

    private void markMigrationApplied() {
        this.configService.write(new V20250804104500_TightenTokenSecurity.MigrationCompleted());
    }

    public record MigrationCompleted() {}
}
