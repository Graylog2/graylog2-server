/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
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
