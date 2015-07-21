package org.graylog.plugins.netflow.flows;

import org.graylog2.plugin.Message;

import javax.annotation.Nullable;

public interface NetFlow {
    String toMessageString();

    @Nullable
    Message toMessage();
}
