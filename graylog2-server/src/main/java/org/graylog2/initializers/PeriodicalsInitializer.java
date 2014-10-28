/**
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
 */
package org.graylog2.initializers;

import com.google.inject.Inject;
import org.graylog2.periodical.Periodicals;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.initializers.Initializer;
import org.graylog2.plugin.initializers.InitializerConfigurationException;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.shared.bindings.InstantiationService;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

public class PeriodicalsInitializer implements Initializer {

    private static final Logger LOG = LoggerFactory.getLogger(PeriodicalsInitializer.class);

    public static final String NAME = "Periodicals initializer";
    private final InstantiationService instantiationService;
    private final Periodicals periodicals;
    private final ServerStatus serverStatus;

    @Inject
    public PeriodicalsInitializer(InstantiationService instantiationService,
                                  Periodicals periodicals,
                                  ServerStatus serverStatus) {
        this.instantiationService = instantiationService;
        this.periodicals = periodicals;
        this.serverStatus = serverStatus;
    }

    @Override
    public void initialize(Map<String, String> config) throws InitializerConfigurationException {
        String packageName = Periodical.class.getPackage().toString();
        Reflections reflections = new Reflections("org.graylog2.shared.periodical");

        for (Class<? extends Periodical> type : reflections.getSubTypesOf(Periodical.class)) {
            try {
                Periodical periodical = instantiationService.getInstance(type);

                periodical.initialize();

                if (periodical.masterOnly() && !serverStatus.hasCapability(ServerStatus.Capability.MASTER)) {
                    LOG.info("Not starting [{}] periodical. Only started on graylog2-server master nodes.", periodical.getClass().getCanonicalName());
                    continue;
                }

                if (!periodical.startOnThisNode()) {
                    LOG.info("Not starting [{}] periodical. Not configured to run on this node.", periodical.getClass().getCanonicalName());
                    continue;
                }

                // Register and start.
                periodicals.registerAndStart(periodical);
            } catch (Exception e) {
                LOG.error("Could not initialize periodical.", e);
            }
        }
    }

    @Override
    public Map<String, String> getRequestedConfiguration() {
        return Collections.emptyMap();
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
