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
package org.graylog.grn;

import org.graylog.grn.providers.DashboardGRNDescriptorProvider;
import org.graylog.grn.providers.FallbackGRNDescriptorProvider;
import org.graylog.grn.providers.StreamGRNDescriptorProvider;
import org.graylog.grn.providers.UserGRNDescriptorProvider;
import org.graylog2.plugin.PluginModule;

public class GRNModule extends PluginModule {
    @Override
    protected void configure() {
        bind(GRNRegistry.class).toInstance(GRNRegistry.createWithBuiltinTypes());

        // TODO: Implement missing GRN descriptor providers
        addGRNType(GRNTypes.BUILTIN_TEAM, FallbackGRNDescriptorProvider.class);
        addGRNType(GRNTypes.COLLECTION, FallbackGRNDescriptorProvider.class);
        addGRNType(GRNTypes.DASHBOARD, DashboardGRNDescriptorProvider.class);
        addGRNType(GRNTypes.EVENT_DEFINITION, FallbackGRNDescriptorProvider.class);
        addGRNType(GRNTypes.GRANT, FallbackGRNDescriptorProvider.class);
        addGRNType(GRNTypes.ROLE, FallbackGRNDescriptorProvider.class);
        addGRNType(GRNTypes.STREAM, StreamGRNDescriptorProvider.class);
        addGRNType(GRNTypes.USER, UserGRNDescriptorProvider.class);
    }
}
