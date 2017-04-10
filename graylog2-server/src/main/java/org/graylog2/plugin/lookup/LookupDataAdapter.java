package org.graylog2.plugin.lookup;

import javax.annotation.Nullable;

public abstract class LookupDataAdapter {
    private String id;

    @Nullable
    public String id() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public abstract Object get(Object key);

    public abstract void set(Object key, Object value);

    public abstract Class<? extends LookupDataAdapterConfiguration> configurationClass();

    public abstract LookupDataAdapterConfiguration defaultConfiguration();

}
