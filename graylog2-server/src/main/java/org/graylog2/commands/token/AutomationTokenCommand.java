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
package org.graylog2.commands.token;

import com.github.rvesse.airline.annotations.Arguments;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.restrictions.Required;
import com.google.inject.Module;
import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import org.graylog2.featureflag.FeatureFlags;
import org.graylog2.security.AccessTokenService;
import org.graylog2.security.AccessTokenServiceImpl;

/**
 * This command will insert/update an automation API token with a fixed ID for the admin user
 */
@Command(name = "automation-token", description = "Adds an automation API token for the local admin user.")
public class AutomationTokenCommand extends AbstractAutomationTokenCommand {

    @Arguments(description = "Value of the automation token to be created (or to replace existing value).")
    @Required
    private String value;

    public AutomationTokenCommand() {
        super("automation-token");
    }

    @Override
    protected @Nonnull List<Module> getNodeCommandBindings(FeatureFlags featureFlags) {
        final ArrayList<Module> modules = new ArrayList<>(super.getNodeCommandBindings(featureFlags));
        modules.add(binder -> binder.bind(AccessTokenService.class).to(AccessTokenServiceImpl.class));
        return modules;
    }

    @Override
    protected void startCommand() {
        injector.getInstance(AutomationTokenCommandExecution.class).run(value);
    }
}
