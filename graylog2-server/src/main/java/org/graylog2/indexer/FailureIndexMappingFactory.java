package org.graylog2.indexer;

import com.github.zafarkhaja.semver.Version;

public interface FailureIndexMappingFactory {
    IndexMappingTemplate failureIndexMappingFor(Version elasticsearchVersion);
}
