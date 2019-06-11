package org.graylog.plugins.views.migrations;

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.rest.ViewsRestPermissions;
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
                ViewsRestPermissions.VIEW_USE,
                ViewsRestPermissions.VIEW_CREATE,
                ViewsRestPermissions.EXTENDEDSEARCH_USE,
                ViewsRestPermissions.EXTENDEDSEARCH_CREATE
        ));
    }
}
