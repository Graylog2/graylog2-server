package org.graylog2.agents;

import java.util.List;

public interface AgentService {
    long count();
    Agent save(Agent agent);
    List<Agent> all();
    Agent findById(String id);
    List<Agent> findByNodeId(String nodeId);
    int destroy(Agent agent);
}
