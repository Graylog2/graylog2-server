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
package org.graylog2.cluster.leader;

import com.google.inject.Scopes;
import org.graylog2.Configuration;
import org.graylog2.plugin.PluginModule;

public class LeaderElectionModule extends PluginModule {

    private final Configuration configuration;

    public LeaderElectionModule(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void configure() {
        final String leaderElectionMode = configuration.getLeaderElectionMode();

        switch (leaderElectionMode) {
            case "legacy":
                bind(LeaderElectionService.class).to(LegacyLeaderElectionService.class).in(Scopes.SINGLETON);
                break;
            case "manual":
                bind(LeaderElectionService.class).to(ManualLeaderElectionService.class).in(Scopes.SINGLETON);
                serviceBinder().addBinding().to(ManualLeaderElectionService.class).in(Scopes.SINGLETON);
                break;
            case "lock-based":
                bind(LeaderElectionService.class).to(LockBasedLeaderElectionService.class).in(Scopes.SINGLETON);
                serviceBinder().addBinding().to(LockBasedLeaderElectionService.class).in(Scopes.SINGLETON);
                break;
            case "atomix":
                bind(LeaderElectionService.class).to(AtomixLeaderElectionService.class).in(Scopes.SINGLETON);
                serviceBinder().addBinding().to(AtomixLeaderElectionService.class).in(Scopes.SINGLETON);
                break;
            default:
                throw new IllegalArgumentException("Unknown leader election mode \"" + leaderElectionMode + "\".");
        }
    }
}
