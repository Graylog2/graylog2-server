package org.graylog.testing.containermatrix;

import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.graylog.testing.mongodb.MongoDBContainer;

public interface ContainerVersions {
    String DEFAULT_ES = ElasticsearchInstance.DEFAULT_VERSION;
    String DEFAULT_MONGO = MongoDBContainer.DEFAULT_VERSION;

    String ES7 = DEFAULT_ES;
    String ES6 = "6.8.4";

    String MONGO3 = "3.6";
    String MONGO4 = "4.0";
}
