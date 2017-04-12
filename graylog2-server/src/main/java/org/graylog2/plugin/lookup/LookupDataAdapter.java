package org.graylog2.plugin.lookup;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.graylog2.lookup.LookupTable;

import javax.annotation.Nullable;

public abstract class LookupDataAdapter {

    private String id;

    private LookupTable lookupTable;

    @Nullable
    public String id() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Nullable
    public LookupTable getLookupTable() {
        return lookupTable;
    }

    public void setLookupTable(LookupTable lookupTable) {
        this.lookupTable = lookupTable;
    }

    public abstract Object get(Object key);

    public abstract void set(Object key, Object value);

    public interface Factory<T extends LookupDataAdapter> {
        T create(LookupDataAdapterConfiguration configuration);

        Descriptor getDescriptor();
    }

    public abstract static class Descriptor<C extends LookupDataAdapterConfiguration> {

        private final String type;
        private final Class<C> configClass;

        public Descriptor(String type, Class<C> configClass) {
            this.type = type;
            this.configClass = configClass;
        }

        @JsonProperty("type")
        public String getType() {
            return type;
        }

        @JsonProperty("config_class")
        public Class<C> getConfigClass() {
            return configClass;
        }

        @JsonProperty("default_config")
        public abstract C defaultConfiguration();

    }

}
