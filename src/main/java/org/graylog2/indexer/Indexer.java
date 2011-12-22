package org.graylog2.indexer;

import org.graylog2.Main;
import org.graylog2.messagehandlers.gelf.GELFMessage;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 */
public abstract class Indexer {
    public abstract boolean bulkIndex(List<GELFMessage> messages, Set<String> types);

    public abstract boolean deleteMessagesByTimeRange(int to);
    
    protected String buildElasticSearchURL() {
		return Main.configuration.getElasticSearchUrl();
	}

	protected String buildIndexURL() {
		return buildElasticSearchURL() + ElasticSearchHttpIndexer.INDEX;
	}

	public abstract boolean createIndex() throws IOException;
}
