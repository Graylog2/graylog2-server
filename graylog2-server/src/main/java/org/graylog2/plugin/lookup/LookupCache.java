/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.plugin.lookup;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.assistedinject.Assisted;
import org.graylog2.lookup.LookupTable;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkState;

public abstract class LookupCache extends AbstractIdleService {

    private String id;

    private LookupTable lookupTable;

    private final String name;
    private final LookupCacheConfiguration config;

    protected LookupCache(String id,
                          String name,
                          LookupCacheConfiguration config) {
        this.id = id;
        this.name = name;
        this.config = config;
    }

    @Override
    protected void startUp() throws Exception {
        doStart();
    }

    protected abstract void doStart() throws Exception;

    @Override
    protected void shutDown() throws Exception {
        doStop();
    }

    protected abstract void doStop() throws Exception;

    @Nullable
    public String id() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LookupTable getLookupTable() {
        checkState(lookupTable != null, "lookup table cannot be null");
        return lookupTable;
    }

    public void setLookupTable(LookupTable lookupTable) {
        this.lookupTable = lookupTable;
    }

    public abstract LookupResult get(Object key);

    public abstract LookupResult getIfPresent(Object key);

    public abstract void set(Object key, Object retrievedValue);

    public abstract void purge();

    public abstract void purge(Object key);

    public LookupCacheConfiguration getConfig() {
        return config;
    }

    public String name() {
        return name;
    }

    public interface Factory<T extends LookupCache> {
        T create(@Assisted("id") String id, @Assisted("name") String name, LookupCacheConfiguration configuration);

        Descriptor getDescriptor();
    }

    public abstract static class Descriptor<C extends LookupCacheConfiguration> {

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
