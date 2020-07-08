package org.graylog.security;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;

public class UserContextBinder extends AbstractBinder {
    @Override
    protected void configure() {
        bindFactory(UserContextFactory.class)
                .to(UserContext.class)
//                .proxy(true)
//                .proxyForSameScope(false)
                .in(RequestScoped.class);
    }
}
