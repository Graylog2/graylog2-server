package org.graylog.storage.elasticsearch7;

import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.indices.template.delete.DeleteIndexTemplateRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.IndexTemplatesExistRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.PutIndexTemplateRequest;
import org.graylog2.indexer.indices.Template;

import javax.inject.Inject;
import java.util.Map;

public class LegacyIndexTemplateAdapter implements IndexTemplateAdapter {
    private final ElasticsearchClient client;

    @Inject
    public LegacyIndexTemplateAdapter(ElasticsearchClient client) {
        this.client = client;
    }

    @Override
    public boolean ensureIndexTemplate(String templateName, Template template) {
        final Map<String, Object> templateSource = Map.of(
                "index_patterns", template.indexPatterns(),
                "mappings", template.mappings(),
                "settings", template.settings(),
                "order", template.order()
        );
        final PutIndexTemplateRequest request = new PutIndexTemplateRequest(templateName)
                .source(templateSource);

        final AcknowledgedResponse result = client.execute((c, requestOptions) -> c.indices().putTemplate(request, requestOptions),
                "Unable to create index template " + templateName);

        return result.isAcknowledged();
    }

    @Override
    public boolean indexTemplateExists(String templateName) {
        return client.execute((c, requestOptions) -> c.indices().existsTemplate(new IndexTemplatesExistRequest(templateName),
                requestOptions), "Unable to verify index template existence " + templateName);
    }

    @Override
    public boolean deleteIndexTemplate(String templateName) {
        final DeleteIndexTemplateRequest request = new DeleteIndexTemplateRequest(templateName);

        final AcknowledgedResponse result = client.execute((c, requestOptions) -> c.indices().deleteTemplate(request, requestOptions),
                "Unable to delete index template " + templateName);
        return result.isAcknowledged();
    }
}
