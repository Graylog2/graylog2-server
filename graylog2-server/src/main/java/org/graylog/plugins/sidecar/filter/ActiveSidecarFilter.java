package org.graylog.plugins.sidecar.filter;

import org.graylog.plugins.sidecar.rest.models.Sidecar;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;

import java.util.function.Predicate;

public class ActiveSidecarFilter implements Predicate<Sidecar> {
    private final Period timeoutPeriod;

    public ActiveSidecarFilter(Period timeoutPeriod) {
        this.timeoutPeriod = timeoutPeriod;
    }

    @Override
    public boolean test(Sidecar sidecar) {
        final DateTime threshold = DateTime.now(DateTimeZone.UTC).minus(timeoutPeriod);
        return sidecar.lastSeen().isAfter(threshold);
    }
}
