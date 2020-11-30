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
package org.graylog.plugins.sidecar;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Scopes;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import org.graylog.plugins.sidecar.audit.SidecarAuditEventTypes;
import org.graylog.plugins.sidecar.common.SidecarPluginConfiguration;
import org.graylog.plugins.sidecar.filter.AdministrationFilter;
import org.graylog.plugins.sidecar.filter.CollectorAdministrationFilter;
import org.graylog.plugins.sidecar.filter.ConfigurationAdministrationFilter;
import org.graylog.plugins.sidecar.filter.OsAdministrationFilter;
import org.graylog.plugins.sidecar.filter.StatusAdministrationFilter;
import org.graylog.plugins.sidecar.migrations.V20180212165000_AddDefaultCollectors;
import org.graylog.plugins.sidecar.migrations.V20180323150000_AddSidecarUser;
import org.graylog.plugins.sidecar.migrations.V20180601151500_AddDefaultConfiguration;
import org.graylog.plugins.sidecar.periodical.PurgeExpiredConfigurationUploads;
import org.graylog.plugins.sidecar.periodical.PurgeExpiredSidecarsThread;
import org.graylog.plugins.sidecar.permissions.SidecarRestPermissions;
import org.graylog.plugins.sidecar.services.CollectorService;
import org.graylog.plugins.sidecar.services.ConfigurationService;
import org.graylog.plugins.sidecar.services.ConfigurationVariableService;
import org.graylog.plugins.sidecar.services.EtagService;
import org.graylog.plugins.sidecar.services.SidecarService;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.PluginConfigBean;
import org.graylog2.plugin.PluginModule;

import java.util.Set;

public class SidecarModule extends PluginModule {
    @Override
    public Set<? extends PluginConfigBean> getConfigBeans() {
        return ImmutableSet.of(
                new SidecarPluginConfiguration()
        );
    }

    @Override
    protected void configure() {
        bind(ConfigurationService.class).asEagerSingleton();
        bind(SidecarService.class).asEagerSingleton();
        bind(CollectorService.class).asEagerSingleton();
        bind(ConfigurationVariableService.class).asEagerSingleton();

        install(new FactoryModuleBuilder()
                .implement(AdministrationFilter.class, Names.named("collector"), CollectorAdministrationFilter.class)
                .implement(AdministrationFilter.class, Names.named("configuration"), ConfigurationAdministrationFilter.class)
                .implement(AdministrationFilter.class, Names.named("os"), OsAdministrationFilter.class)
                .implement(AdministrationFilter.class, Names.named("status"), StatusAdministrationFilter.class)
                .build(AdministrationFilter.Factory.class));

        registerRestControllerPackage(getClass().getPackage().getName());
        addPermissions(SidecarRestPermissions.class);
        addPeriodical(PurgeExpiredSidecarsThread.class);
        addPeriodical(PurgeExpiredConfigurationUploads.class);

        addAuditEventTypes(SidecarAuditEventTypes.class);

        final Multibinder<Migration> binder = Multibinder.newSetBinder(binder(), Migration.class);
        binder.addBinding().to(V20180212165000_AddDefaultCollectors.class);
        binder.addBinding().to(V20180323150000_AddSidecarUser.class);
        binder.addBinding().to(V20180601151500_AddDefaultConfiguration.class);

        serviceBinder().addBinding().to(EtagService.class).in(Scopes.SINGLETON);
    }
}
