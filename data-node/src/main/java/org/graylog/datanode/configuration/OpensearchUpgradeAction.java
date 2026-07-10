package org.graylog.datanode.configuration;

import com.github.zafarkhaja.semver.Version;
import jakarta.inject.Inject;
import org.graylog2.cluster.nodes.DataNodeMetadataService;
import org.graylog2.plugin.system.NodeId;

public class OpensearchUpgradeAction {

    private final DataNodeMetadataService metadataService;
    private final NodeId nodeId;

    @Inject
    public OpensearchUpgradeAction(DataNodeMetadataService metadataService, NodeId nodeId) {
        this.metadataService = metadataService;
        this.nodeId = nodeId;
    }

    public boolean upgradeToLatestAvaiable() {
        return metadataService.findByNodeId(nodeId.getNodeId())
                .filter(m -> m.latestAvailableOpensearchVersion() != null)
                .filter(m -> Version.parse(m.latestAvailableOpensearchVersion()).isHigherThan(Version.parse(m.currentOpensearchVersion())))
                .map(
                        dataNodeMetadata -> {
                            metadataService.setOpensearchVersions(nodeId.getNodeId(), dataNodeMetadata.latestAvailableOpensearchVersion(), null);
                            return true;
                        })
                .orElse(false);
    }
}
