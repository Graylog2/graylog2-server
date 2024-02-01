/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
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
