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
package org.graylog.scheduler;

import com.google.inject.multibindings.OptionalBinder;
import org.graylog.scheduler.audit.JobSchedulerAuditEventTypes;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog.scheduler.clock.JobSchedulerSystemClock;
import org.graylog2.plugin.PluginConfigBean;
import org.graylog2.plugin.PluginModule;

import java.util.Collections;
import java.util.Set;

/**
 * Job scheduler specific bindings.
 */
public class JobSchedulerModule extends PluginModule {
    @Override
    public Set<? extends PluginConfigBean> getConfigBeans() {
        return Collections.singleton(new JobSchedulerConfiguration());
    }

    @Override
    protected void configure() {
        bind(JobSchedulerService.class).asEagerSingleton();
        bind(JobSchedulerClock.class).toInstance(JobSchedulerSystemClock.INSTANCE);

        OptionalBinder.newOptionalBinder(binder(), JobSchedulerConfig.class)
                .setDefault().to(DefaultJobSchedulerConfig.class);

        // Add all rest resources in this package
        registerRestControllerPackage(getClass().getPackage().getName());

        addInitializer(JobSchedulerService.class);
        addAuditEventTypes(JobSchedulerAuditEventTypes.class);
    }
}
