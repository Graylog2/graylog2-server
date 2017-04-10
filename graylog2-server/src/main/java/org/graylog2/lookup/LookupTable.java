package org.graylog2.lookup;

import com.google.auto.value.AutoValue;

import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.lookup.LookupCache;
import org.graylog2.plugin.lookup.LookupDataAdapter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@AutoValue
@WithBeanGetter
public abstract class LookupTable {

    @Nullable
    public abstract String id();

    public abstract String title();

    public abstract String description();

    public abstract String name();

    public abstract LookupCache cacheProvider();

    public abstract LookupDataAdapter dataProvider();

    public static Builder builder() {
        return new AutoValue_LookupTable.Builder();
    }

    @Nullable
    public Object lookup(@Nonnull Object key) {

        return dataProvider().get(key);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(String id);

        public abstract Builder title(String title);

        public abstract Builder description(String description);

        public abstract Builder name(String name);

        public abstract Builder cacheProvider(LookupCache cacheProvider);

        public abstract Builder dataProvider(LookupDataAdapter dataProvider);

        public abstract LookupTable build();
    }
}
