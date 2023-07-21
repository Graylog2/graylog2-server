package org.graylog.testing.completebackend;

import org.graylog2.storage.SearchVersion;

public interface SearchServerInterfaceProvider {
    SearchServerBuilder getBuilderFor(SearchVersion version);
}
