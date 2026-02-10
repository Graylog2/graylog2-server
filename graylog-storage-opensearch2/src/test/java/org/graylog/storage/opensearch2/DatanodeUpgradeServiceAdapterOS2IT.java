package org.graylog.storage.opensearch2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zafarkhaja.semver.Version;
import org.graylog.plugins.datanode.DatanodeUpgradeServiceAdapter;
import org.graylog.plugins.datanode.DatanodeUpgradeServiceAdapterIT;
import org.graylog.storage.opensearch2.testing.OpenSearchInstance;
import org.graylog.testing.elasticsearch.SearchInstance;

class DatanodeUpgradeServiceAdapterOS2IT extends DatanodeUpgradeServiceAdapterIT {

    @SearchInstance
    public final OpenSearchInstance openSearchInstance = OpenSearchInstance.create();

    @Override
    protected DatanodeUpgradeServiceAdapter createAdapter() {
        return new DatanodeUpgradeServiceAdapterOS2(openSearchInstance.openSearchClient(), new ObjectMapper());
    }

    @Override
    protected Version indexerVersion() {
        return openSearchInstance.version().version();
    }
}
