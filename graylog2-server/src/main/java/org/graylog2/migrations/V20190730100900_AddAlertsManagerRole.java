package org.graylog2.migrations;

import com.google.common.collect.ImmutableSet;
import org.graylog2.shared.security.RestPermissions;

import javax.inject.Inject;
import java.time.ZonedDateTime;

public class V20190730100900_AddAlertsManagerRole extends Migration {
    private final MigrationHelpers helpers;

    @Inject
    public V20190730100900_AddAlertsManagerRole(MigrationHelpers helpers) {
        this.helpers = helpers;
    }
    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2019-07-30T10:09:00Z");
    }

    @Override
    public void upgrade() {
        helpers.ensureBuiltinRole("Alerts Manager", "Allows reading and writing all event definitions and event notifications (built-in)", ImmutableSet.of(
                RestPermissions.EVENT_DEFINITIONS_CREATE,
                RestPermissions.EVENT_DEFINITIONS_DELETE,
                RestPermissions.EVENT_DEFINITIONS_EDIT,
                RestPermissions.EVENT_DEFINITIONS_EXECUTE,
                RestPermissions.EVENT_DEFINITIONS_READ,
                RestPermissions.EVENT_NOTIFICATIONS_CREATE,
                RestPermissions.EVENT_NOTIFICATIONS_DELETE,
                RestPermissions.EVENT_NOTIFICATIONS_EDIT,
                RestPermissions.EVENT_NOTIFICATIONS_READ
        ));
    }
}
