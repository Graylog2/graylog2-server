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
package org.graylog2.bootstrap.commands;

import com.github.rvesse.airline.annotations.Command;
import org.graylog2.bootstrap.CliCommand;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.Version;

@Command(name = "version", description = "Show the Graylog and JVM versions")
public class ShowVersion implements CliCommand {
    private final Version version = Version.CURRENT_CLASSPATH;

    @Override
    public void run() {
        System.out.println("Graylog " + version);
        System.out.println("JRE: " + Tools.getSystemInformation());
    }
}
