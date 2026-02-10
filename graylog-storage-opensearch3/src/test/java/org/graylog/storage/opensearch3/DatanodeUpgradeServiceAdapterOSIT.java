package org.graylog.storage.opensearch3;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zafarkhaja.semver.Version;
import org.graylog.plugins.datanode.DatanodeUpgradeServiceAdapter;
import org.graylog.plugins.datanode.DatanodeUpgradeServiceAdapterIT;
import org.graylog.storage.opensearch3.testing.OpenSearchInstance;
import org.graylog.testing.elasticsearch.SearchInstance;

class DatanodeUpgradeServiceAdapterOSIT extends DatanodeUpgradeServiceAdapterIT {

    @SearchInstance
    public final OpenSearchInstance openSearchInstance = OpenSearchInstance.create();

    @Override
    protected DatanodeUpgradeServiceAdapter createAdapter() {
        return new DatanodeUpgradeServiceAdapterOS(openSearchInstance.getOfficialOpensearchClient(), new ObjectMapper());
    }

    @Override
    protected Version indexerVersion() {
        return openSearchInstance.version().version();
    }
}
