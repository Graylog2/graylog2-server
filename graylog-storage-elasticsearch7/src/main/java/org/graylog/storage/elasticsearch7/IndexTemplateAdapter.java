package org.graylog.storage.elasticsearch7;

import org.graylog2.indexer.indices.Template;

public interface IndexTemplateAdapter {
    boolean ensureIndexTemplate(String templateName, Template template);

    boolean indexTemplateExists(String templateName);

    boolean deleteIndexTemplate(String templateName);
}
