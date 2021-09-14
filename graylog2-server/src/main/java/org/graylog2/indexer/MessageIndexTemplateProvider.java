package org.graylog2.indexer;

import com.github.zafarkhaja.semver.Version;
import org.graylog2.indexer.indexset.IndexSetConfig;

public class MessageIndexTemplateProvider implements IndexTemplateProvider {

    @Override
    public IndexMapping forVersion(Version elasticsearchVersion) {
        if (elasticsearchVersion.satisfies("^5.0.0")) {
            return new IndexMapping5();
        } else if (elasticsearchVersion.satisfies("^6.0.0")) {
            return new IndexMapping6();
        } else if (elasticsearchVersion.satisfies("^7.0.0")) {
            return new IndexMapping7();
        } else {
            throw new ElasticsearchException("Unsupported Elasticsearch version: " + elasticsearchVersion);
        }
    }

    @Override
    public String templateType() {
        return IndexSetConfig.TemplateType.MESSAGES;
    }
}
