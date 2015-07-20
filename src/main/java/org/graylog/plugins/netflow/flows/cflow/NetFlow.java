package org.graylog.plugins.netflow.flows.cflow;

import org.graylog2.plugin.Message;
import org.graylog2.plugin.journal.RawMessage;

import javax.annotation.Nullable;

public interface NetFlow {
    String toMessageString();

    @Nullable
    Message toMessage(RawMessage rawMessage);
}
