package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport;

import com.eaio.uuid.UUIDGen;

import javax.inject.Inject;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class RandomUUIDProvider {
    private final AtomicLong date;
    private final long clockSeqAndNode;

    @Inject
    public RandomUUIDProvider(Date date) {
        this(date, UUIDGen.getClockSeqAndNode());
    }

    public RandomUUIDProvider(Date date, long clockSeqAndNode) {
        this.date = new AtomicLong(date.getTime());
        this.clockSeqAndNode = clockSeqAndNode;
    }

    public String get() {
        return new UUID(date.getAndIncrement(), this.clockSeqAndNode).toString();
    }
}
