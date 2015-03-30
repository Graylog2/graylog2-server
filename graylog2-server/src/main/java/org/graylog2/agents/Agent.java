package org.graylog2.agents;

import org.joda.time.DateTime;

public interface Agent {
    String getId();
    String getNodeId();
    String getOperatingSystem();
    DateTime getLastSeen();
}
