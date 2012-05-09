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

import org.apache.log4j.Logger;
import org.graylog2.Configuration;
import org.graylog2.GraylogServer;
import org.graylog2.RulesEngine;

/**
 * DroolsInitializer.java: Apr 11, 2012 5:19:03 PM
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class DroolsInitializer implements Initializer {

    private static final Logger LOG = Logger.getLogger(DroolsInitializer.class);

    private final Configuration configuration;
    private final GraylogServer graylogServer;

    public DroolsInitializer(GraylogServer server, Configuration configuration) {
        this.graylogServer = server;
        this.configuration = configuration;
    }

    @Override
    public void initialize() {
        try {
            String rulesFilePath = configuration.getDroolsRulesFile();
            if (rulesFilePath != null && !rulesFilePath.isEmpty()) {
                RulesEngine drools = new RulesEngine();
                drools.addRules(rulesFilePath);
                graylogServer.setRulesEngine(drools);
                LOG.info("Using rules: " + rulesFilePath);
            } else {
                LOG.info("Not using rules");
            }
        } catch (Exception e) {
            LOG.fatal("Could not load rules engine: " + e.getMessage(), e);
            System.exit(1);
        }
    }

}