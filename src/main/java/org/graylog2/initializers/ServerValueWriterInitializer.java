/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
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

import java.util.Map;
import org.graylog2.plugin.initializers.Initializer;
import org.graylog2.Core;
import org.graylog2.HostSystem;
import org.graylog2.ServerValue;
import org.graylog2.plugin.Tools;
import org.graylog2.plugins.PluginRegistry;
import org.graylog2.periodical.ServerValueWriterThread;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.initializers.InitializerConfigurationException;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class ServerValueWriterInitializer extends SimpleFixedRateScheduleInitializer implements Initializer {

    private static final String NAME = "Server values";
    
    @Override
    public void initialize(GraylogServer server, Map<String, String> config) throws InitializerConfigurationException {
        Core srv = (Core) server;
        // Write some initial values. Only done once.
        ServerValue serverValue = srv.getServerValues();
        serverValue.setStartupTime(Tools.getUTCTimestamp());
        serverValue.setPID(Integer.parseInt(Tools.getPID()));
        serverValue.setJREInfo(Tools.getSystemInformation());
        serverValue.setGraylog2Version(Core.GRAYLOG2_VERSION);
        serverValue.setAvailableProcessors(HostSystem.getAvailableProcessors());
        serverValue.setLocalHostname(Tools.getLocalCanonicalHostname());
        serverValue.setIsMaster(srv.isMaster());

        if (srv.isMaster()) {
            PluginRegistry.setActiveTransports(srv, srv.getTransports());
            PluginRegistry.setActiveAlarmCallbacks(srv, srv.getAlarmCallbacks());
            PluginRegistry.setActiveMessageOutputs(srv, srv.getOutputs());
            PluginRegistry.setActiveMessageInputs(srv, srv.getInputs());
            PluginRegistry.setActiveInitializers(srv, srv.getInitializers());
        }
        
        configureScheduler(
                srv,
                new ServerValueWriterThread(srv),
                ServerValueWriterThread.INITIAL_DELAY,
                ServerValueWriterThread.PERIOD
        );
    }

    @Override
    public Map<String, String> getRequestedConfiguration() {
        // Built in initializer. This is just for plugin compat. No special configuration required.
        return com.google.common.collect.Maps.newHashMap();
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