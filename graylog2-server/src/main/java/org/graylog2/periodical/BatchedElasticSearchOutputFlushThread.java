package org.graylog2.periodical;

import org.graylog2.Configuration;
import org.graylog2.outputs.BatchedElasticSearchOutput;
import org.graylog2.plugin.outputs.MessageOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class BatchedElasticSearchOutputFlushThread extends Periodical {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return false;
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
        return core.getConfiguration().getOutputFlushInterval();
    }

    @Override
    public void run() {
        for (MessageOutput output : core.outputs().get()) {
            if (output instanceof BatchedElasticSearchOutput) {
                BatchedElasticSearchOutput batchedOutput = (BatchedElasticSearchOutput)output;
                try {
                    batchedOutput.flush();
                } catch (Exception e) {
                    LOG.error("Caught exception while trying to flush output: {}", e);
                }
            }
        }
    }
}
