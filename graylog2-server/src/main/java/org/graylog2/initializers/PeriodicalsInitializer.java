/**
 * Copyright 2014 Lennart Koopmann <lennart@torch.sh>
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
import org.graylog2.periodical.Periodical;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.initializers.Initializer;
import org.graylog2.plugin.initializers.InitializerConfigurationException;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class PeriodicalsInitializer implements Initializer {

    private static final Logger LOG = LoggerFactory.getLogger(PeriodicalsInitializer.class);

    public static final String NAME = "Periodicals initializer";

    @Override
    public void initialize(GraylogServer server, Map<String, String> config) throws InitializerConfigurationException {
        Core core = (Core) server;
        Reflections reflections = new Reflections("org.graylog2.periodical");

        for (Class<? extends Periodical> type : reflections.getSubTypesOf(Periodical.class)) {
            try {
                Periodical periodical = type.newInstance();

                periodical.initialize(core);

                if (periodical.masterOnly() && !core.isMaster()) {
                    LOG.info("Not starting [{}] periodical. Only started on graylog2-server master nodes.", periodical.getClass().getCanonicalName());
                    continue;
                }

                if (!periodical.startOnThisNode()) {
                    LOG.info("Not starting [{}] periodical. Not configured to run on this node.", periodical.getClass().getCanonicalName());
                    continue;
                }

                // Register and start.
                core.periodicals().registerAndStart(periodical);
            } catch (Exception e) {
                LOG.error("Could not initialize periodical.", e);
            }
        }
    }

    @Override
    public Map<String, String> getRequestedConfiguration() {
        return Maps.newHashMap();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean masterOnly() {
        return false;
    }
}
