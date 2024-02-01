package org.graylog2.system.processing.control;

import java.util.Set;

import static org.graylog2.shared.utilities.StringUtils.f;

public class OutputBufferDrainFailureException extends RuntimeException {

    public OutputBufferDrainFailureException(long retryIntervalSeconds, int maxRetries, Set<String> nodes) {
        super(f("Failed to drain the Output buffer on nodes [%s] within [%s] seconds. " +
                        "This probably means that your Graylog nodes are processing a very high rate of messages. " +
                        "You can try to pause processing manually in System > Nodes, and attempt the installation " +
                        "again once the output buffers have fully drained.",
                String.join("", nodes), retryIntervalSeconds * maxRetries));
    }
}
