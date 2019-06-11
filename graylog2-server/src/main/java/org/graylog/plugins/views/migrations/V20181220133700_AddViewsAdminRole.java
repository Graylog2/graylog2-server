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
                ViewsRestPermissions.VIEW_USE,
                ViewsRestPermissions.VIEW_CREATE,
                ViewsRestPermissions.VIEW_READ,
                ViewsRestPermissions.VIEW_EDIT,
                ViewsRestPermissions.EXTENDEDSEARCH_USE,
                ViewsRestPermissions.EXTENDEDSEARCH_CREATE
        ));
    }
}
