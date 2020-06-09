package org.graylog.storage.elasticsearch6;

import com.github.joschi.jadconfig.util.Duration;
import org.graylog2.indexer.cluster.ClusterAdapter;
import org.graylog2.indexer.cluster.ClusterIT;

public class ClusterES6IT extends ClusterIT {
    @Override
    protected ClusterAdapter clusterAdapter(Duration timeout) {
        return new ClusterAdapterES6(jestClient(), timeout);
    }
}
