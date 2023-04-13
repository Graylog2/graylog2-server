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

import com.github.rvesse.airline.Cli;
import com.github.rvesse.airline.builder.CliBuilder;
import com.google.common.collect.ImmutableSet;
import org.graylog.datanode.bootstrap.commands.CliCommandHelp;
import org.graylog.datanode.bootstrap.commands.ShowVersion;
import org.graylog.security.certutil.CertutilCa;
import org.graylog.security.certutil.CertutilCert;
import org.graylog.security.certutil.CertutilHttp;
import org.graylog2.bootstrap.CliCommand;
import org.graylog2.bootstrap.CliCommandsProvider;

import java.util.ServiceLoader;

public class Main {
    public static void main(String[] args) {
        final CliBuilder<CliCommand> builder = Cli.<CliCommand>builder("graylog")
                .withDescription("Open source, centralized log management")
                .withDefaultCommand(CliCommandHelp.class)
                .withCommands(ImmutableSet.of(
                        CertutilCa.class,
                        CertutilCert.class,
                        CertutilHttp.class,
                        ShowVersion.class,
                        CliCommandHelp.class));

        // add rest from classpath
        final ServiceLoader<CliCommandsProvider> commandsProviders = ServiceLoader.load(CliCommandsProvider.class);
        for (CliCommandsProvider provider : commandsProviders) {
            provider.addTopLevelCommandsOrGroups(builder);
        }

        final Cli<CliCommand> cli = builder.build();
        final Runnable command = cli.parse(args);

        command.run();
    }
}
