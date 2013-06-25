/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.graylog2.initializers;

import com.google.common.collect.Maps;
import org.graylog2.Core;
import org.graylog2.periodical.NodePingThread;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.initializers.Initializer;
import org.graylog2.plugin.initializers.InitializerConfigurationException;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class NodePingInitializer extends SimpleFixedRateScheduleInitializer implements Initializer {

    private static final String NAME = "Node ping";

    @Override
    public void initialize(GraylogServer server, Map<String, String> config) throws InitializerConfigurationException {
        configureScheduler(
                (Core) server,
                new NodePingThread((Core) server),
                NodePingThread.INITIAL_DELAY,
                NodePingThread.PERIOD
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
