package org.graylog2.shared.bindings;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import org.graylog2.plugin.ServerStatus;

import java.util.Set;

public class ServerStatusBindings extends AbstractModule {

    private final Set<ServerStatus.Capability> capabilities;

    public ServerStatusBindings(Set<ServerStatus.Capability> capabilities) {
        this.capabilities = capabilities;
    }

    @Override
    protected void configure() {
        Multibinder<ServerStatus.Capability> capabilityBinder = Multibinder.newSetBinder(binder(), ServerStatus.Capability.class);
        for(ServerStatus.Capability capability : capabilities) {
            capabilityBinder.addBinding().toInstance(capability);
        }

        bind(ServerStatus.class).in(Scopes.SINGLETON);
    }
}
