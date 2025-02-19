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
 * Updates {@link UserConfiguration} in the DB to contain relaxed defaults for restricting creation of API-tokens
 * and a default TTL for creating new ones.
 *
 * This migration is supposed to run only for minor updates (e.g. to 6.2). For the next major update (i.e. to 7.0),
 * another migration should take over to restrict creation of API-tokens to only internal users and/or admins.
 * The default TTL however should always be the same. It is important though to also consider existing settings for
 * token management.
 */
public class V20250219134200_DefaultTTLForNewTokens extends Migration {
    private final ClusterConfigService configService;
    private final boolean isFreshInstallation;

    @Inject
    public V20250219134200_DefaultTTLForNewTokens(ClusterConfigService configService, @Named("isFreshInstallation") boolean isFreshInstallation) {
        this.configService = configService;
        this.isFreshInstallation = isFreshInstallation;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2025-02-19T13:42:00Z");
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
                // Actually, this should not happen, as the migration V20250206105400_TokenManagementConfiguration
                // should be executed before, where some defaults are set.
                configToUpdate = newDefaults;
            } else {
                //A UserConfig already exists. As V20250206105400_TokenManagementConfiguration should have run already,
                // which did set defaults restricting externals and admins, respectively, we only need to add the new default TTL:
                configToUpdate = UserConfiguration.create(
                        configToUpdate.enableGlobalSessionTimeout(),
                        configToUpdate.globalSessionTimeoutInterval(),
                        configToUpdate.allowAccessTokenForExternalUsers(),
                        configToUpdate.restrictAccessTokenToAdmins(),
                        newDefaults.defaultTTLForNewTokens());
            }

            configService.write(configToUpdate);
        }

        markMigrationApplied();
    }

    private boolean migrationAlreadyApplied() {
        return Objects.nonNull(configService.get(V20250219134200_DefaultTTLForNewTokens.MigrationCompleted.class));
    }

    private void markMigrationApplied() {
        this.configService.write(new V20250219134200_DefaultTTLForNewTokens.MigrationCompleted());
    }

    public record MigrationCompleted() {}
}
