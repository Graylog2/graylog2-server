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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.graylog2.Configuration;
import org.graylog2.Core;
import org.graylog2.RulesEngineImpl;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class DroolsInitializer implements Initializer {

    private static final Logger LOG = LoggerFactory.getLogger(DroolsInitializer.class);

    private final Configuration configuration;
    private final Core graylogServer;

    public DroolsInitializer(Core server, Configuration configuration) {
        this.graylogServer = server;
        this.configuration = configuration;
    }

    @Override
    public void initialize() {
        try {
            String rulesFilePath = configuration.getDroolsRulesFile();
            if (rulesFilePath != null && !rulesFilePath.isEmpty()) {
                RulesEngineImpl drools = new RulesEngineImpl();
                drools.addRules(rulesFilePath);
                graylogServer.setRulesEngine(drools);
                LOG.info("Using rules: {}", rulesFilePath);
            } else {
                LOG.info("Not using rules");
            }
        } catch (Exception e) {
            LOG.error("Could not load rules engine: " + e.getMessage(), e);
            System.exit(1);
        }
    }
    
    @Override
    public boolean masterOnly() {
        return false;
    }

}