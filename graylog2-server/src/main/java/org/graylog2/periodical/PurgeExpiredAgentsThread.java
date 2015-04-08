package org.graylog2.periodical;

import com.github.joschi.jadconfig.util.Duration;
import com.google.common.primitives.Ints;
import org.graylog2.Configuration;
import org.graylog2.agents.AgentService;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class PurgeExpiredAgentsThread extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(PurgeExpiredAgentsThread.class);
    private final AgentService agentService;
    private final Configuration configuration;

    @Inject
    public PurgeExpiredAgentsThread(AgentService agentService,
                                    Configuration configuration) {
        this.agentService = agentService;
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
        return true;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return 60*60;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public void doRun() {
        final Duration threshold = configuration.getAgentExpirationThreshold();
        agentService.destroyExpired(Ints.checkedCast(threshold.getQuantity()), threshold.getUnit());
    }
}
