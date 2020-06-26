package org.graylog.security;

import com.google.inject.multibindings.OptionalBinder;
import org.graylog2.plugin.PluginModule;

public class SecurityModule extends PluginModule {
    @Override
    protected void configure() {

        bind(BuiltinRoles.class).asEagerSingleton();

        OptionalBinder.newOptionalBinder(binder(), GrantPermissionResolver.class)
                .setDefault().to(DefaultGrantPermissionResolver.class);

        // Add all rest resources in this package
        // TODO: Check if we need to use addRestResource() here for the final version to make sure
        //       we get the path prefix. Do we want this?
        registerRestControllerPackage(getClass().getPackage().getName());
    }
}
