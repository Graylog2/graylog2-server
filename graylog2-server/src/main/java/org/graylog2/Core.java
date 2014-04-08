/*
 * Copyright 2012-2014 TORCH GmbH
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
 */

package org.graylog2;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ServiceManager;
import org.graylog2.initializers.ServiceManagerListener;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.graylog2.shared.ServerStatus;
import org.graylog2.system.activities.Activity;
import org.graylog2.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * Server core, handling and holding basically everything.
 * 
 * (Du kannst das Geraet nicht bremsen, schon garnicht mit bloßen Haenden.)
 * 
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class Core {

    private static final Logger LOG = LoggerFactory.getLogger(Core.class);

    @Inject
    private Configuration configuration;
    @Inject
    private ServerStatus serverStatus;

    @Inject
    private ActivityWriter activityWriter;

    @Inject
    private ServiceManager serviceManager;

    @Inject
    private ServiceManagerListener serviceManagerListener;

    public void initialize() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                String msg = "SIGNAL received. Shutting down.";
                LOG.info(msg);
                activityWriter.write(new Activity(msg, Core.class));

                serverStatus.setLifecycle(Lifecycle.HALTING);

                serviceManager.stopAsync().awaitStopped();

                /*GracefulShutdown gs = new GracefulShutdown(serverStatus, activityWriter, configuration,
                        bufferSynchronizer, cacheSynchronizer, indexer, periodicals, inputs);
                gs.run();*/
            }
        });
    }

    public void run() {
        // Ramp it all up. (both plugins and built-in types)
        serviceManager.addListener(serviceManagerListener, MoreExecutors.sameThreadExecutor());
        serviceManager.startAsync().awaitHealthy();
    }
}
