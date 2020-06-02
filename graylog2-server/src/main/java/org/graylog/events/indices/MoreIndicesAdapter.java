package org.graylog.events.indices;

import org.graylog.events.event.Event;

import java.util.List;
import java.util.Map;

public interface MoreIndicesAdapter {
    void bulkIndex(List<Map.Entry<String, Event>> requests);
}
