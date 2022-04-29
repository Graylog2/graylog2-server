package org.graylog2.storage.providers;

import org.graylog2.indexer.fieldtypes.streams.AggregationBasedFieldTypeFilterAdapter;
import org.graylog2.storage.DetectedSearchVersion;
import org.graylog2.storage.SearchVersion;
import org.graylog2.storage.VersionAwareProvider;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;

public class AggregationBasedFieldTypeFilterAdapterProvider extends VersionAwareProvider<AggregationBasedFieldTypeFilterAdapter> {

    @Inject
    public AggregationBasedFieldTypeFilterAdapterProvider(@DetectedSearchVersion SearchVersion version,
                                                          Map<SearchVersion, Provider<AggregationBasedFieldTypeFilterAdapter>> pluginBindings) {
        super(version, pluginBindings);
    }
}
