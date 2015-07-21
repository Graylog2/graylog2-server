package org.graylog.plugins.netflow.flows;

import java.util.Collection;

public interface NetFlowPacket {
    Collection<NetFlow> getFlows();
}
