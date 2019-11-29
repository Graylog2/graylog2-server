package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport;

import org.bson.types.ObjectId;

import javax.inject.Inject;
import java.util.Date;

public class RandomObjectIdProvider {
    private final Date date;

    @Inject
    public RandomObjectIdProvider(Date date) {
        this.date = date;
    }

    public String get() {
        return new ObjectId(this.date).toHexString();
    }
}
