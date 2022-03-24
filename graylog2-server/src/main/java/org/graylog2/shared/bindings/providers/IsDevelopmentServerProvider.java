package org.graylog2.shared.bindings.providers;

import javax.inject.Provider;

public class IsDevelopmentServerProvider implements Provider<Boolean> {
    private final boolean isDevelopmentServer;

    public IsDevelopmentServerProvider() {
        final String development = System.getenv("DEVELOPMENT");
        this.isDevelopmentServer = !(development == null || development.equalsIgnoreCase("false"));
    }

    @Override
    public Boolean get() {
        return this.isDevelopmentServer;
    }
}
