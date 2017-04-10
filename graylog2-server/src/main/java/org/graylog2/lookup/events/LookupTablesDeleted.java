package org.graylog2.lookup.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.Set;

@AutoValue
public abstract class LookupTablesDeleted {
    @JsonProperty("lookup_table_ids")
    public abstract Set<String> lookupTableIds();

    @JsonProperty("lookup_table_names")
    public abstract Set<String> lookupTableNames();

    @JsonCreator
    public static LookupTablesDeleted create(@JsonProperty("lookup_table_ids") Set<String> lookupTableIds,
                                             @JsonProperty("lookup_table_names") Set<String> lookupTableNames) {
        return new AutoValue_LookupTablesDeleted(lookupTableIds, lookupTableNames);
    }
}
