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
package org.graylog2.commands;

import com.github.rvesse.airline.annotations.Command;
import com.google.inject.Module;
import org.graylog2.featureflag.FeatureFlags;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.List;


/**
 * This command will start an example node that waits for a few seconds and then stops again.
 * It's a demonstration of a minimal Graylog node.
 */
@Command(name = "example", description = "Start an example node for a Graylog cluster")
public class ExampleCommand extends AbstractNodeCommand {
    private static final Logger LOG = LoggerFactory.getLogger(ExampleCommand.class);

    @Override
    public void run() {
        super.run();
    }

    @Override
    protected void startCommand() {
        LOG.info("Starting example node");
        try {
            Thread.sleep(30_000);
        } catch (InterruptedException e) {
            LOG.info("Interrupted...");
            Thread.currentThread().interrupt();
        }
        LOG.info("Stopping example node");
    }

    @Override
    protected List<Object> getCommandConfigurationBeans() {
        return List.of();
    }

    @Override
    protected @Nonnull List<Module> getNodeCommandBindings(final FeatureFlags featureFlags) {
        return List.of();
    }

    @NotNull
    @Override
    protected List<Object> getNodeCommandConfigurationBeans() {
        return List.of();
    }
}
