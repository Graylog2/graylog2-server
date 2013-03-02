/**
 * Copyright 2013 Lennart Koopmann <lennart@socketfeed.com>
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
import java.util.Map;
import org.graylog2.Core;
import org.graylog2.periodical.MasterCacheWorkerThread;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.initializers.Initializer;
import org.graylog2.plugin.initializers.InitializerConfigurationException;

/**
 * 
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class MasterCacheWorkersInitializer implements Initializer {

    public static final String NAME = "MasterCache workers initializer";
    
    @Override
    public void initialize(GraylogServer server, Map<String, String> config) throws InitializerConfigurationException {
        Core core = (Core) server;
        
        // Input cache worker.
        core.getScheduler().submit(
                new MasterCacheWorkerThread(core, core.getInputCache(), core.getProcessBuffer())
        );
        
        // Output cache worker.
        core.getScheduler().submit(
                new MasterCacheWorkerThread(core, core.getOutputCache(), core.getOutputBuffer())
        );
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
