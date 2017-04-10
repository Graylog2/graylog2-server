package org.graylog2.lookup;

import com.google.auto.value.AutoValue;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.lookup.LookupDataAdapter;
import org.graylog2.plugin.lookup.LookupDataAdapterConfiguration;

public class DevZeroDataAdapter extends LookupDataAdapter {

    public static final String NAME = "dev_zero";

    @Override
    public Object get(Object key) {
        return null;
    }

    @Override
    public void set(Object key, Object value) {

    }

    @Override
    public Class<? extends LookupDataAdapterConfiguration> configurationClass() {
        return Config.class;
    }

    @Override
    public LookupDataAdapterConfiguration defaultConfiguration() {
        return Config.builder().build();
    }

    @AutoValue
    @WithBeanGetter
    @JsonAutoDetect
    @JsonDeserialize(builder = AutoValue_DevZeroDataAdapter_Config.Builder.class)
    public static abstract class Config implements LookupDataAdapterConfiguration {

        @Override
        public abstract String type();

        public static Builder builder() {
            return new AutoValue_DevZeroDataAdapter_Config.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder type(String type);

            public abstract Config build();
        }
    }
}
