package org.graylog.plugin.filter.geoipresolver;

import org.graylog2.plugin.Message;
import org.graylog2.plugin.filters.MessageFilter;

public class GeoIpResolverFilter implements MessageFilter {
    @Override
    public boolean filter(Message message) {
        return false;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public int getPriority() {
        return 0;
    }
}
