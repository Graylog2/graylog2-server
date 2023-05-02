package org.graylog.plugins.sidecar.migrations;

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.sidecar.permissions.SidecarRestPermissions;
import org.graylog2.migrations.Migration;
import org.graylog2.migrations.MigrationHelpers;

import javax.inject.Inject;
import java.time.ZonedDateTime;

public class V20230502164900_AddSidecarManagerRole extends Migration {

    private final MigrationHelpers helpers;

    @Inject
    public V20230502164900_AddSidecarManagerRole(MigrationHelpers migrationHelpers) {
        this.helpers = migrationHelpers;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2018-03-23T15:00:00Z");
    }

    @Override
    public void upgrade() {
        helpers.ensureBuiltinRole(
                "Sidecar Manager",
                "Grants access to read, register and pull configurations for Sidecars (built-in)",
                ImmutableSet.of(
                        SidecarRestPermissions.COLLECTORS_READ,
                        SidecarRestPermissions.CONFIGURATIONS_READ,
                        SidecarRestPermissions.SIDECARS_READ,
                        SidecarRestPermissions.SIDECARS_UPDATE));

    }
}
