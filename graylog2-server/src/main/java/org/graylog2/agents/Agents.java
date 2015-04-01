package org.graylog2.agents;

import com.google.common.collect.Lists;
import org.graylog2.rest.models.agent.responses.AgentSummary;

import java.util.List;

public class Agents {
    public static List<AgentSummary> toSummaryList(List<Agent> agents) {
        final List<AgentSummary> agentSummaries = Lists.newArrayListWithCapacity(agents.size());
        for (Agent agent : agents)
            agentSummaries.add(agent.toSummary());

        return agentSummaries;
    }
}
