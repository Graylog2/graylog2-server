package org.graylog2.storage.providers;

import org.graylog2.indexer.fieldtypes.streams.CountExistingBasedFieldTypeFilterAdapter;
import org.graylog2.storage.DetectedSearchVersion;
import org.graylog2.storage.SearchVersion;
import org.graylog2.storage.VersionAwareProvider;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;

public class CountExistingBasedFieldTypeFilterAdapterProvider extends VersionAwareProvider<CountExistingBasedFieldTypeFilterAdapter> {

    @Inject
    public CountExistingBasedFieldTypeFilterAdapterProvider(@DetectedSearchVersion SearchVersion version,
                                                            Map<SearchVersion, Provider<CountExistingBasedFieldTypeFilterAdapter>> pluginBindings) {
        super(version, pluginBindings);
    }
}
