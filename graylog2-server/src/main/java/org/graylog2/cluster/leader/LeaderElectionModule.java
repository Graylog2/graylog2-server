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
            case "fixed":
                bind(LeaderElectionService.class).to(FixedLeaderElectionService.class).in(Scopes.SINGLETON);
                serviceBinder().addBinding().to(FixedLeaderElectionService.class).in(Scopes.SINGLETON);
                break;
            case "lock-based":
                bind(LeaderElectionService.class).to(LockBasedLeaderElectionService.class).in(Scopes.SINGLETON);
                serviceBinder().addBinding().to(LockBasedLeaderElectionService.class).in(Scopes.SINGLETON);
                break;
            default:
                throw new IllegalArgumentException("Unknown leader election mode \"" + leaderElectionMode + "\".");
        }
    }
}
