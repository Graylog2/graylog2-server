package org.graylog2.lookup;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import org.graylog2.lookup.events.LookupTablesDeleted;
import org.graylog2.lookup.events.LookupTablesUpdated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LookupTableService {
    private static final Logger LOG = LoggerFactory.getLogger(LookupTableService.class);

    private final MongoLutService mongoLutService;
    private final ConcurrentMap<String, LookupTable> lookupTables = new ConcurrentHashMap<>();

    @Inject
    public LookupTableService(MongoLutService mongoLutService, EventBus serverEventBus) {
        this.mongoLutService = mongoLutService;

        // Initialize all lookup tables before subscribing to events
        initialize();

        // TODO: This object should have life cycle management. For now it needs to be a singleton to avoid leaking references in the event bus.
        serverEventBus.register(this);
    }

    private void initialize() {
        // Load all lookup table, data provider and cache DTOs and create a LookupTable object for each
        //mongoLutService.findAll().forEach(entry -> lookupTables.put(entry.name(), compute(entry.name())));
    }

    private LookupTable compute(String name) {
        // Build the actual lookup table object tree and store in the concurrent map
        return null;
    }

    @Subscribe
    public void handleLookupTableUpdate(LookupTablesUpdated event) {
        event.lookupTableNames().forEach(name -> lookupTables.put(name, compute(name)));
    }

    @Subscribe
    public void handleLookupTableDeletion(LookupTablesDeleted event) {
        event.lookupTableNames().forEach(lookupTables::remove);
    }

    public Builder newBuilder() {
        return new Builder(this);
    }

    @Nullable
    private LookupTable getTable(String name) {
        final LookupTable lookupTable = lookupTables.get(name);
        if (lookupTable == null) {
            LOG.warn("Lookup table <{}> does not exist", name);
        }
        return lookupTable;
    }

    public static class Builder {
        private final LookupTableService lookupTableService;

        private String lookupTableName;
        private Object defaultValue = null;

        public Builder(LookupTableService lookupTableService) {
            this.lookupTableService = lookupTableService;
        }

        public Builder lookupTable(String name) {
            this.lookupTableName = name;
            return this;
        }

        public Builder defaultValue(Object value) {
            this.defaultValue = value;
            return this;
        }

        public Function build() {
            return new Function(lookupTableService.getTable(lookupTableName), defaultValue);
        }
    }

    public static class Function {
        private final LookupTable lookupTable;
        private final Object defaultValue;

        public Function(@Nullable LookupTable lookupTable, @Nullable Object defaultValue) {
            this.lookupTable = lookupTable;
            this.defaultValue = defaultValue;
        }

        @Nullable
        public Object lookup(@Nonnull String key) {
            if (lookupTable == null) {
                return defaultValue;
            }

            final Object value = lookupTable.lookup(key);

            if (value == null) {
                return defaultValue;
            }

            return value;
        }
    }
}
