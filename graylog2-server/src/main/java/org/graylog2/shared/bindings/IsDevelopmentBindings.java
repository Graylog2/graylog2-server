package org.graylog2.shared.bindings;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.name.Names;
import org.graylog2.shared.bindings.providers.IsDevelopmentServerProvider;

public class IsDevelopmentBindings implements Module {
    @Override
    public void configure(Binder binder) {
        binder.bind(Boolean.class).annotatedWith(Names.named("isDevelopmentServer")).toProvider(IsDevelopmentServerProvider.class);
    }
}
