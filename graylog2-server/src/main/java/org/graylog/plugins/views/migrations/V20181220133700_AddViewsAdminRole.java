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
import org.graylog.plugins.views.search.rest.ViewsRestPermissions;
import org.graylog2.migrations.Migration;
import org.graylog2.migrations.MigrationHelpers;

import javax.inject.Inject;
import java.time.ZonedDateTime;

public class V20181220133700_AddViewsAdminRole extends Migration {
    private final MigrationHelpers helpers;

    @Inject
    public V20181220133700_AddViewsAdminRole(MigrationHelpers helpers) {
        this.helpers = helpers;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2018-12-20T13:37:00Z");
    }

    @Override
    public void upgrade() {
        helpers.ensureBuiltinRole("Views Manager", "Allows reading and writing all views and extended searches (built-in)", ImmutableSet.of(
                LegacyPermissions.VIEW_USE,
                LegacyPermissions.VIEW_CREATE,
                ViewsRestPermissions.VIEW_READ,
                ViewsRestPermissions.VIEW_EDIT,
                LegacyPermissions.EXTENDEDSEARCH_USE,
                LegacyPermissions.EXTENDEDSEARCH_CREATE
        ));
    }
}
