package org.graylog.storage.elasticsearch7;

import com.codahale.metrics.Timer;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.GetMappingsRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.GetMappingsResponse;
import org.graylog2.indexer.fieldtypes.FieldTypeDTO;
import org.graylog2.indexer.fieldtypes.IndexFieldTypePollerAdapter;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class IndexFieldTypePollerAdapterES7 implements IndexFieldTypePollerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(IndexFieldTypePollerAdapterES7.class);
    private final ElasticsearchClient client;

    @Inject
    public IndexFieldTypePollerAdapterES7(ElasticsearchClient client) {
        this.client = client;
    }

    @Override
    public Optional<Set<FieldTypeDTO>> pollIndex(String indexName, Timer pollTimer) {
        final GetMappingsRequest request = new GetMappingsRequest()
                .indices(indexName);

        final GetMappingsResponse mappingsResponse;
        try (final Timer.Context ignored = pollTimer.time()) {
            mappingsResponse = client.execute((c, requestOptions) -> c.indices().getMapping(request, requestOptions));
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.error("Couldn't get mapping for index <{}>", indexName, e);
            } else {
                LOG.error("Couldn't get mapping for index <{}>: {}", indexName, ExceptionUtils.getRootCauseMessage(e));
            }
            return Optional.empty();
        }

        final Map<String, String> fieldTypes = mappingsResponse.mappings().entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().type()));
        return Optional.of(
                fieldTypes.entrySet()
                        .stream()
                        // The "type" value is empty if we deal with a nested data type
                        // TODO: Figure out how to handle nested fields, for now we only support the top-level fields
                        .filter(field -> !field.getValue().isEmpty())
                        .map(field -> FieldTypeDTO.create(field.getKey(), field.getValue()))
                        .collect(Collectors.toSet())
        );
    }
}
