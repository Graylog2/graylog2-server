package org.graylog.scheduler;

import com.google.inject.multibindings.OptionalBinder;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog.scheduler.clock.JobSchedulerSystemClock;
import org.graylog2.plugin.PluginModule;

/**
 * Job scheduler specific bindings.
 */
public class JobSchedulerModule extends PluginModule {
    @Override
    protected void configure() {
        bind(JobSchedulerService.class).asEagerSingleton();
        bind(JobSchedulerClock.class).toInstance(JobSchedulerSystemClock.INSTANCE);

        OptionalBinder.newOptionalBinder(binder(), JobSchedulerConfig.class)
                .setDefault().to(DefaultJobSchedulerConfig.class);

        // Add all rest resources in this package
        registerRestControllerPackage(getClass().getPackage().getName());

        addInitializer(JobSchedulerService.class);
    }
}
