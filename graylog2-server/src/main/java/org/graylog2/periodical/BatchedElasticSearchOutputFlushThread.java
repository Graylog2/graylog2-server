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
package org.graylog2.periodical;

import org.graylog2.Configuration;
import org.graylog2.outputs.BlockingBatchedESOutput;
import org.graylog2.outputs.OutputRegistry;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class BatchedElasticSearchOutputFlushThread extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(BatchedElasticSearchOutputFlushThread.class);
    private final OutputRegistry outputRegistry;
    private final Configuration configuration;

    @Inject
    public BatchedElasticSearchOutputFlushThread(OutputRegistry outputRegistry, Configuration configuration) {
        this.outputRegistry = outputRegistry;
        this.configuration = configuration;
    }

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return true;
    }

    @Override
    public boolean masterOnly() {
        return false;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return false;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return configuration.getOutputFlushInterval();
    }

    @Override
    public void doRun() {
        LOG.debug("Checking for outputs to flush ...");
        for (MessageOutput output : outputRegistry.getMessageOutputs()) {
            if (output instanceof BlockingBatchedESOutput) {
                try {
                    LOG.debug("Flushing output <{}>", output);
                    ((BlockingBatchedESOutput) output).forceFlushIfTimedout();
                } catch (Exception e) {
                    LOG.error("Caught exception while trying to flush output: {}", e);
                }
            }
        }
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
