package org.graylog2.commands.Token;

import com.github.joschi.jadconfig.Parameter;
import com.google.inject.Module;
import jakarta.annotation.Nonnull;
import java.util.List;
import org.graylog2.Configuration;
import org.graylog2.bindings.ConfigurationModule;
import org.graylog2.cluster.ClusterConfigServiceImpl;
import org.graylog2.commands.AbstractNodeCommand;
import org.graylog2.featureflag.FeatureFlags;
import org.graylog2.plugin.cluster.ClusterConfigService;

public abstract class AbstractTokenCommand extends AbstractNodeCommand {
    public AbstractTokenCommand(String commandName) {
        super(commandName, new TokenCommandConfiguration());
    }

    @Override
    protected @Nonnull List<Module> getNodeCommandBindings(final FeatureFlags featureFlags) {
        final TokenCommandConfiguration tokenCommandConfiguration = (TokenCommandConfiguration) configuration;
        return List.of(binder -> {
            binder.install(new ConfigurationModule(tokenCommandConfiguration));
            binder.bind(ClusterConfigService.class).to(ClusterConfigServiceImpl.class).asEagerSingleton();
        });
    }

    @Override
    protected @Nonnull List<Object> getNodeCommandConfigurationBeans() {
        return List.of();
    }

    private static class TokenCommandConfiguration extends Configuration {
        @Parameter("password_secret")
        String passwordSecret;

        @Override
        public boolean withPlugins() {
            return false;
        }

        @Override
        public boolean withInputs() {
            return false;
        }

    }
}
