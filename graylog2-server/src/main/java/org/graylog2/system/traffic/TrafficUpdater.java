package org.graylog2.system.traffic;

import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;

public interface TrafficUpdater {
    void updateTraffic(DateTime observationTime, NodeId nodeId, long inLastMinute, long outLastMinute, long decodedLastMinute);
}
