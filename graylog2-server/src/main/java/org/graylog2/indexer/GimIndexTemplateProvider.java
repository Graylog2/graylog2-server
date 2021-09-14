package org.graylog2.indexer;

import com.github.zafarkhaja.semver.Version;
import org.graylog2.indexer.indexset.IndexSetConfig;

public class GimIndexTemplateProvider implements IndexTemplateProvider {

    @Override
    public IndexMappingTemplate forVersion(Version elasticsearchVersion) {
        if (elasticsearchVersion.satisfies("^6.0.0")) {
            return new GIMMapping6();
        } else if (elasticsearchVersion.satisfies("^7.0.0")) {
            return new GIMMapping7();
        } else {
            throw new ElasticsearchException("Unsupported Elasticsearch version: " + elasticsearchVersion);
        }
    }

    @Override
    public String templateType() {
        return IndexSetConfig.TemplateType.GIM_V1;
    }
}
