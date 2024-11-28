/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.datanode.bootstrap;

import com.github.rvesse.airline.annotations.Option;
import org.graylog.datanode.Configuration;
import org.graylog2.bootstrap.CmdLineTool;

public abstract class DatanodeCmdLineTool extends CmdLineTool<Configuration> {

    @Option(name = {"-f", "--configfile"}, description = "Configuration file for Graylog Data Node", override = true)
    protected String configFile = "/etc/graylog/datanode/datanode.conf";

    @Option(name = {"-ff", "--featureflagfile"}, description = "Configuration file for Graylog Data Node feature flags", override = true)
    protected String customFeatureFlagFile = "/etc/graylog/datanode/feature-flag.conf";

    protected String commandName = "command";
    
    protected DatanodeCmdLineTool(String commandName) {
        super(commandName, new Configuration());
    }

}
