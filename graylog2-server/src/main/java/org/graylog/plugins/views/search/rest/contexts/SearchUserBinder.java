package org.graylog.plugins.views.search.rest.contexts;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.graylog.plugins.views.search.permissions.SearchUser;

public class SearchUserBinder extends AbstractBinder {
    @Override
    protected void configure() {
        bindFactory(SearchUserFactory.class)
                .to(SearchUser.class)
                .in(RequestScoped.class);
    }
}
