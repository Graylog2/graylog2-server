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

import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.OptionalBinder;
import org.graylog.security.authservice.AuthServiceBackend;
import org.graylog.security.authservice.AuthServiceBackendConfig;
import org.graylog.security.authservice.InternalAuthServiceBackend;
import org.graylog.security.authservice.ProvisionerAction;
import org.graylog.security.authservice.backend.ADAuthServiceBackend;
import org.graylog.security.authservice.backend.ADAuthServiceBackendConfig;
import org.graylog.security.authservice.backend.LDAPAuthServiceBackend;
import org.graylog.security.authservice.backend.LDAPAuthServiceBackendConfig;
import org.graylog.security.authservice.backend.MongoDBAuthServiceBackend;
import org.graylog.security.authservice.ldap.UnboundLDAPConnector;
import org.graylog.security.shares.DefaultGranteeService;
import org.graylog.security.shares.GranteeService;
import org.graylog2.plugin.PluginModule;

public class SecurityModule extends PluginModule {
    @Override
    protected void configure() {
        // Call the following to ensure the presence of the multi binder and avoid startup errors when no action is registered
        MapBinder.newMapBinder(
                binder(),
                TypeLiteral.get(String.class),
                new TypeLiteral<ProvisionerAction.Factory<? extends ProvisionerAction>>() {}
        );
        authServiceBackendBinder();

        bind(BuiltinCapabilities.class).asEagerSingleton();
        bind(UnboundLDAPConnector.class).in(Scopes.SINGLETON);

        install(new FactoryModuleBuilder().implement(GranteeAuthorizer.class, GranteeAuthorizer.class).build(GranteeAuthorizer.Factory.class));

        OptionalBinder.newOptionalBinder(binder(), PermissionAndRoleResolver.class)
                .setDefault().to(DefaultPermissionAndRoleResolver.class);

        OptionalBinder.newOptionalBinder(binder(), GranteeService.class)
                .setDefault().to(DefaultGranteeService.class);

        bind(AuthServiceBackend.class).annotatedWith(InternalAuthServiceBackend.class).to(MongoDBAuthServiceBackend.class);

        // Add all rest resources in this package
        // TODO: Check if we need to use addRestResource() here for the final version to make sure
        //       we get the path prefix. Do we want this?
        registerRestControllerPackage(getClass().getPackage().getName());

        addAuditEventTypes(SecurityAuditEventTypes.class);

        addAuthServiceBackend(LDAPAuthServiceBackend.TYPE_NAME,
                LDAPAuthServiceBackend.class,
                LDAPAuthServiceBackend.Factory.class,
                LDAPAuthServiceBackendConfig.class);
        addAuthServiceBackend(ADAuthServiceBackend.TYPE_NAME,
                ADAuthServiceBackend.class,
                ADAuthServiceBackend.Factory.class,
                ADAuthServiceBackendConfig.class);
    }

    private MapBinder<String, AuthServiceBackend.Factory<? extends AuthServiceBackend>> authServiceBackendBinder() {
        return MapBinder.newMapBinder(
                binder(),
                TypeLiteral.get(String.class),
                new TypeLiteral<AuthServiceBackend.Factory<? extends AuthServiceBackend>>() {}
        );
    }

    protected void addAuthServiceBackend(String name,
                                         Class<? extends AuthServiceBackend> backendClass,
                                         Class<? extends AuthServiceBackend.Factory<? extends AuthServiceBackend>> factoryClass,
                                         Class<? extends AuthServiceBackendConfig> configClass) {
        install(new FactoryModuleBuilder().implement(AuthServiceBackend.class, backendClass).build(factoryClass));
        authServiceBackendBinder().addBinding(name).to(factoryClass);
        registerJacksonSubtype(configClass, name);
    }
}
