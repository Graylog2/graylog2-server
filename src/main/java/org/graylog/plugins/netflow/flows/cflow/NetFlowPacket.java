package org.graylog.plugins.netflow.flows.cflow;

import java.util.Collection;

public interface NetFlowPacket {
    Collection<NetFlow> getFlows();
}
