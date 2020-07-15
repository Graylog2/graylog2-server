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
package org.graylog.security;

import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.OptionalBinder;
import org.graylog.security.shares.DefaultGranteeService;
import org.graylog.security.shares.GranteeService;
import org.graylog2.plugin.PluginModule;

public class SecurityModule extends PluginModule {
    @Override
    protected void configure() {

        bind(BuiltinCapabilities.class).asEagerSingleton();

        install(new FactoryModuleBuilder().implement(GranteeAuthorizer.class, GranteeAuthorizer.class).build(GranteeAuthorizer.Factory.class));

        OptionalBinder.newOptionalBinder(binder(), GrantPermissionResolver.class)
                .setDefault().to(DefaultGrantPermissionResolver.class);

        OptionalBinder.newOptionalBinder(binder(), GranteeService.class)
                .setDefault().to(DefaultGranteeService.class);

        // Add all rest resources in this package
        // TODO: Check if we need to use addRestResource() here for the final version to make sure
        //       we get the path prefix. Do we want this?
        registerRestControllerPackage(getClass().getPackage().getName());
    }
}
