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
import org.graylog.plugins.sidecar.periodical.PurgeExpiredSidecarsThread;
import org.graylog.plugins.sidecar.permissions.SidecarRestPermissions;
import org.graylog.plugins.sidecar.rest.resources.ActionResource;
import org.graylog.plugins.sidecar.rest.resources.AdministrationResource;
import org.graylog.plugins.sidecar.rest.resources.CollectorResource;
import org.graylog.plugins.sidecar.rest.resources.ConfigurationResource;
import org.graylog.plugins.sidecar.rest.resources.SidecarResource;
import org.graylog.plugins.sidecar.services.CollectorService;
import org.graylog.plugins.sidecar.services.ConfigurationService;
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

        install(new FactoryModuleBuilder()
                .implement(AdministrationFilter.class, Names.named("collector"), CollectorAdministrationFilter.class)
                .implement(AdministrationFilter.class, Names.named("configuration"), ConfigurationAdministrationFilter.class)
                .implement(AdministrationFilter.class, Names.named("os"), OsAdministrationFilter.class)
                .implement(AdministrationFilter.class, Names.named("status"), StatusAdministrationFilter.class)
                .build(AdministrationFilter.Factory.class));

        addRestResource(ConfigurationResource.class);
        addRestResource(CollectorResource.class);
        addRestResource(ActionResource.class);
        addRestResource(AdministrationResource.class);
        addRestResource(SidecarResource.class);
        addPermissions(SidecarRestPermissions.class);
        addPeriodical(PurgeExpiredSidecarsThread.class);

        addAuditEventTypes(SidecarAuditEventTypes.class);

        final Multibinder<Migration> binder = Multibinder.newSetBinder(binder(), Migration.class);
        binder.addBinding().to(V20180212165000_AddDefaultCollectors.class);
        binder.addBinding().to(V20180323150000_AddSidecarUser.class);
        binder.addBinding().to(V20180601151500_AddDefaultConfiguration.class);

        serviceBinder().addBinding().to(EtagService.class).in(Scopes.SINGLETON);
    }
}
