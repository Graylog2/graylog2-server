package org.graylog.plugins.views.search.engine.fieldlist;

import org.graylog.plugins.views.search.permissions.SearchUser;

public record QueryAwareFieldListRetrievalParams(SearchUser searchUser,
                                                 int size,
                                                 boolean useSampler,
                                                 int sampleSize) {


    public QueryAwareFieldListRetrievalParams(final SearchUser searchUser) {
        this(searchUser, 1000, false, 100);
    }
}
