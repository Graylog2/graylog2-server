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

import com.google.common.util.concurrent.Service;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import org.graylog2.Configuration;
import org.graylog2.cluster.lock.LockService;
import org.graylog2.cluster.lock.MongoLockService;
import org.graylog2.plugin.PluginModule;

public class LeaderElectionModule extends PluginModule {
    private final Configuration configuration;

    public LeaderElectionModule(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void configure() {

        // TODO move to another module?
        bind(LockService.class).to(MongoLockService.class).in(Scopes.SINGLETON);

        final LeaderElectionMode mode = configuration.getLeaderElectionMode();

        switch (mode) {
            case STATIC:
                bind(LeaderElectionService.class).to(StaticLeaderElectionService.class).in(Scopes.SINGLETON);
                bind(Service.class).annotatedWith(Names.named("LeaderElectionService")).to(StaticLeaderElectionService.class);
                break;
            case AUTOMATIC:
                bind(LeaderElectionService.class).to(AutomaticLeaderElectionService.class).in(Scopes.SINGLETON);
                bind(Service.class).annotatedWith(Names.named("LeaderElectionService")).to(AutomaticLeaderElectionService.class);
                break;
            default:
                throw new IllegalArgumentException("Unknown leader election mode \"" + mode + "\".");
        }
    }
}
