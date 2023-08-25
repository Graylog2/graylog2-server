package org.graylog.storage.opensearch2;

import org.graylog.shaded.opensearch2.org.opensearch.action.admin.indices.template.delete.DeleteIndexTemplateRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.support.master.AcknowledgedResponse;
import org.graylog.shaded.opensearch2.org.opensearch.client.indices.IndexTemplatesExistRequest;
import org.graylog.shaded.opensearch2.org.opensearch.client.indices.PutIndexTemplateRequest;
import org.graylog2.indexer.indices.Template;

import javax.inject.Inject;
import java.util.Map;

public class LegacyIndexTemplateAdapter implements IndexTemplateAdapter {
    private final OpenSearchClient client;

    @Inject
    public LegacyIndexTemplateAdapter(OpenSearchClient client) {
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
