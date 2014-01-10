package org.graylog2.initializers;

import com.google.common.collect.Maps;
import org.graylog2.Core;
import org.graylog2.periodical.ClusterHealthCheckThread;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.initializers.Initializer;
import org.graylog2.plugin.initializers.InitializerConfigurationException;

import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class ClusterHealthCheckInitializer extends SimpleFixedRateScheduleInitializer implements Initializer {
    private static final String NAME = "Node Health Check";

    @Override
    public void initialize(GraylogServer server, Map<String, String> config) throws InitializerConfigurationException {
        configureScheduler(
                (Core) server,
                new ClusterHealthCheckThread((Core) server),
                ClusterHealthCheckThread.INITIAL_DELAY,
                ClusterHealthCheckThread.PERIOD
        );
    }

    @Override
    public Map<String, String> getRequestedConfiguration() {
        // Built in initializer. This is just for plugin compat. No special configuration required.
        return Maps.newHashMap();
    }

    @Override
    public boolean masterOnly() {
        return false;
    }

    @Override
    public String getName() {
        return NAME;
    }
}

