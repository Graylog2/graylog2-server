package org.graylog.storage.elasticsearch6;

import org.graylog2.indexer.counts.CountsAdapter;
import org.graylog2.indexer.counts.CountsIT;

public class CountsES6IT extends CountsIT {
    @Override
    protected CountsAdapter countsAdapter() {
        return new CountsAdapterES6(jestClient());
    }
}
