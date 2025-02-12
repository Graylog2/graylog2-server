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
import jakarta.inject.Named;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.users.UserConfiguration;

import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Updates {@link UserConfiguration} in the DB to contain relaxed defaults for restricting creation of API-tokens.
 *
 * This migration is supposed to run only for minor updates (e.g. to 6.2). For the next major update (i.e. to 7.0),
 * another migration should take over to restrict creation of API-tokens to only internal users and/or admins.
 */
public class V20250206105400_TokenManagementConfiguration extends Migration {
    private final ClusterConfigService configService;
    private final boolean isFreshInstallation;

    //No breaking changes in minor versions (i.e. access must not be limited for upgrades).
    // When it comes to upgrading to 7.0, access should be restricted.

    @Inject
    public V20250206105400_TokenManagementConfiguration(ClusterConfigService configService, @Named("isFreshInstallation") boolean isFreshInstallation) {
        this.configService = configService;
        this.isFreshInstallation = isFreshInstallation;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2025-02-06T10:54:00Z");
    }

    @Override
    public void upgrade() {
        if (migrationAlreadyApplied()) {
            //Migration already ran, nothing more to do.
            return;
        }

        if (isFreshInstallation) {
            //For a fresh installation, there's no risk of existing API-tokens owned by external or non-admin users.
            // Thus, applying strict config:
            configService.write(UserConfiguration.DEFAULT_VALUES);
        } else {
            //No major upgrade, so the permissions are more relaxed to not introduce breaking changes:
            final UserConfiguration newDefaults = UserConfiguration.DEFAULT_VALUES_FOR_UPGRADE;

            UserConfiguration configToUpdate = this.configService.get(UserConfiguration.class);
            if (configToUpdate == null) {
                //No userConfig exists, let's simply save the default for the current version:
                configToUpdate = newDefaults;
            } else {
                //A UserConfig already exists, but as the two new field have just been introduced, they can't exist in the DB yet.
                // So we set the current version's default values for them to the existing config and store all of it again:
                configToUpdate = UserConfiguration.create(
                        configToUpdate.enableGlobalSessionTimeout(),
                        configToUpdate.globalSessionTimeoutInterval(),
                        newDefaults.allowAccessTokenForExternalUsers(),
                        newDefaults.restrictAccessTokenToAdmins());
            }

            configService.write(configToUpdate);
        }

        markMigrationApplied();
    }

    private boolean migrationAlreadyApplied() {
        return Objects.nonNull(configService.get(V20250206105400_TokenManagementConfiguration.MigrationCompleted.class));
    }

    private void markMigrationApplied() {
        this.configService.write(new V20250206105400_TokenManagementConfiguration.MigrationCompleted());
    }

    public record MigrationCompleted() {}
}
