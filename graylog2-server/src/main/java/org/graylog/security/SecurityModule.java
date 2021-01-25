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
package org.graylog.security;

import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.OptionalBinder;
import org.graylog.security.authservice.AuthServiceBackend;
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
}
