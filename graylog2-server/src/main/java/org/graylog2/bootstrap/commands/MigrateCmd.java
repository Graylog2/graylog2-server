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
import org.graylog2.commands.Server;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.Version;
import org.jsoftbiz.utils.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(name = MigrateCmd.MIGRATION_COMMAND, description = "Run Graylog server migrations")
public class MigrateCmd extends Server {
    public static final String MIGRATION_COMMAND = "migrate";

    private static final Logger LOG = LoggerFactory.getLogger(MigrateCmd.class);

    private final Version version = Version.CURRENT_CLASSPATH;

    public MigrateCmd() {
        super(MIGRATION_COMMAND);
    }

    @Override
    protected void startCommand() {
        final OS os = OS.getOs();

        LOG.info("Graylog {} {} migration command", commandName, version);
        LOG.info("JRE: {}", Tools.getSystemInformation());
        LOG.info("Deployment: {}", configuration.getInstallationSource());
        LOG.info("OS: {}", os.getPlatformName());
        LOG.info("Arch: {}", os.getArch());

        try {
            runMigrations();
        } catch (Exception e) {
            LOG.warn("Exception while running migrations", e);
            System.exit(1);
        }

        System.exit(0);
    }
}
