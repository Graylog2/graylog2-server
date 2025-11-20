package org.graylog2.indexer.indices;

public interface IndexTemplateAdapter {
    boolean ensureIndexTemplate(String templateName, Template template);
    boolean indexTemplateExists(String templateName);
    boolean deleteIndexTemplate(String templateName);
}
