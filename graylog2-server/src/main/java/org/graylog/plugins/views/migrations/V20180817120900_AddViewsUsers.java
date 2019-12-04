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
package org.graylog.plugins.views.migrations;

import com.google.common.collect.ImmutableSet;
import org.graylog2.migrations.Migration;
import org.graylog2.migrations.MigrationHelpers;

import javax.inject.Inject;
import java.time.ZonedDateTime;

public class V20180817120900_AddViewsUsers extends Migration {
    private final MigrationHelpers helpers;

    @Inject
    public V20180817120900_AddViewsUsers(MigrationHelpers helpers) {
        this.helpers = helpers;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2018-08-17T12:09:00Z");
    }

    @Override
    public void upgrade() {
        helpers.ensureBuiltinRole("Views User", "Allows using views and extended searches (built-in)", ImmutableSet.of(
                LegacyPermissions.VIEW_USE,
                LegacyPermissions.VIEW_CREATE,
                LegacyPermissions.EXTENDEDSEARCH_USE,
                LegacyPermissions.EXTENDEDSEARCH_CREATE
        ));
    }
}
