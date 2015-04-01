package org.graylog2.agents;

import org.graylog2.rest.models.agent.responses.AgentSummary;
import org.joda.time.DateTime;

public interface Agent {
    String getId();
    String getNodeId();
    String getOperatingSystem();
    DateTime getLastSeen();

    AgentNodeDetails nodeDetails();

    AgentSummary toSummary();
}
