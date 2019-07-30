/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
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
