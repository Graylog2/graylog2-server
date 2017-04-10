package org.graylog2.plugin.lookup;

import javax.annotation.Nullable;

public abstract class LookupCache {

    private String id;

    @Nullable
    public String id() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public abstract Class<? extends LookupCacheConfiguration> configurationClass();

    public abstract LookupCacheConfiguration defaultConfiguration();

}
