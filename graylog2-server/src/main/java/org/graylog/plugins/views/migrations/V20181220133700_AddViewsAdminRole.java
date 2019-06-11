package org.graylog.plugins.views.migrations;

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.rest.EnterpriseSearchRestPermissions;
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
                EnterpriseSearchRestPermissions.VIEW_USE,
                EnterpriseSearchRestPermissions.VIEW_CREATE,
                EnterpriseSearchRestPermissions.VIEW_READ,
                EnterpriseSearchRestPermissions.VIEW_EDIT,
                EnterpriseSearchRestPermissions.EXTENDEDSEARCH_USE,
                EnterpriseSearchRestPermissions.EXTENDEDSEARCH_CREATE
        ));
    }
}
