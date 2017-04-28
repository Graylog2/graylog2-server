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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.concurrent.locks.ReentrantLock;

import static com.google.common.base.Preconditions.checkState;

public abstract class LookupDataAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(LookupDataAdapter.class);

    private String id;
    private volatile boolean started = false;
    private volatile boolean failed = false;
    private LookupTable lookupTable;
    private ReentrantLock lock = new ReentrantLock();

    private final LookupDataAdapterConfiguration config;

    protected LookupDataAdapter(LookupDataAdapterConfiguration config) {
        this.config = config;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isFailed() {
        return failed;
    }

    public void start() {
        if (started) {
            return;
        }
        lock.lock();
        try {
            doStart();
            started = true;
            failed = false;
        } catch (Exception e) {
            LOG.error("Couldn't start data adapter", e);
            failed = true;
        } finally {
            lock.unlock();
        }
    }
    protected abstract void doStart() throws Exception;

    public void stop() {
        if (!started) {
            return;
        }
        lock.lock();
        try {
            doStop();
            started = false;
        } catch (Exception e) {
            LOG.error("Couldn't stop data adapter", e);
            failed = true;
        } finally {
            lock.unlock();
        }
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

    public LookupResult get(Object key) {
        if (failed) {
            return LookupResult.empty();
        }
        checkState(started, "Data adapter needs to be started before it can be used");
        return doGet(key);
    }
    protected abstract LookupResult doGet(Object key);

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
