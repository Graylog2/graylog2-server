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
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkState;

public abstract class LookupDataAdapter extends AbstractIdleService {
    private static final Logger LOG = LoggerFactory.getLogger(LookupDataAdapter.class);

    private final String id;
    private final String name;

    private final LookupDataAdapterConfiguration config;

    private AtomicReference<Throwable> dataSourceError = new AtomicReference<>();

    protected LookupDataAdapter(String id, String name, LookupDataAdapterConfiguration config) {
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

    /**
     * Returns the refresh interval for this data adapter. Use {@link Duration#ZERO} if refresh should be disabled.
     * @return the refresh interval
     */
    public abstract Duration refreshInterval();

    public void refresh(LookupCachePurge cachePurge) {
        try {
            doRefresh(cachePurge);
        } catch (Exception e) {
            LOG.error("Couldn't refresh data adapter", e);
        }
    }

    protected abstract void doRefresh(LookupCachePurge cachePurge) throws Exception;

    protected void clearError() {
        dataSourceError.set(null);
    }

    public Optional<Throwable> getError() {
        return Optional.ofNullable(dataSourceError.get());
    }

    protected void setError(Throwable throwable) {
        dataSourceError.set(throwable);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public LookupResult get(Object key) {
        if (state() == State.FAILED) {
            return LookupResult.empty();
        }
        checkState(isRunning(), "Data adapter needs to be started before it can be used");
        return doGet(key);
    }
    protected abstract LookupResult doGet(Object key);

    public abstract void set(Object key, Object value);

    public LookupDataAdapterConfiguration getConfig() {
        return config;
    }


    public interface Factory<T extends LookupDataAdapter> {
        T create(@Assisted("id") String id, @Assisted("name") String name, LookupDataAdapterConfiguration configuration);

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
