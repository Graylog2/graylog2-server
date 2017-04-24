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

import org.graylog2.lookup.LookupTable;

import javax.annotation.Nullable;

public abstract class LookupDataAdapter {

    private String id;

    private LookupTable lookupTable;

    private final LookupDataAdapterConfiguration config;

    protected LookupDataAdapter(LookupDataAdapterConfiguration config) {
        this.config = config;
    }

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

    public abstract LookupResult get(Object key);

    public abstract void set(Object key, Object value);

    public LookupDataAdapterConfiguration getConfig() {
        return config;
    }

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
