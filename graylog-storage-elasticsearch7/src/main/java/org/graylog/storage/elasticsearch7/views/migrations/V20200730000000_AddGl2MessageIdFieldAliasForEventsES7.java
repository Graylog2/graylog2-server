package org.graylog.storage.elasticsearch7.views.migrations;

import com.google.common.collect.ImmutableMap;
import org.graylog.plugins.views.migrations.V20200730000000_AddGl2MessageIdFieldAliasForEvents;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RequestOptions;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RestHighLevelClient;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.PutMappingRequest;
import org.graylog2.indexer.ElasticsearchException;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Set;

import static org.graylog2.plugin.Message.FIELD_GL2_MESSAGE_ID;

public class V20200730000000_AddGl2MessageIdFieldAliasForEventsES7 implements V20200730000000_AddGl2MessageIdFieldAliasForEvents.ElasticsearchAdapter {

    private final RestHighLevelClient client;

    @Inject
    public V20200730000000_AddGl2MessageIdFieldAliasForEventsES7(RestHighLevelClient client) {
        this.client = client;
    }

    @Override
    public void addGl2MessageIdFieldAlias(Set<String> indexPrefixes) {

        final String[] prefixesWithWildcard = indexPrefixes.stream().map(p -> p + "*").toArray(String[]::new);

        final PutMappingRequest putMappingRequest = new PutMappingRequest(prefixesWithWildcard)
                .source(ImmutableMap.of("properties", ImmutableMap.of(FIELD_GL2_MESSAGE_ID, aliasMapping())));

        try {
            final AcknowledgedResponse acknowledgedResponse = client.indices().putMapping(putMappingRequest, RequestOptions.DEFAULT);
            if (!acknowledgedResponse.isAcknowledged()) {
                throw new ElasticsearchException(errorMsgFor(prefixesWithWildcard) + " Elasticsearch failed to acknowledge.");
            }
        } catch (IOException e) {
            throw new ElasticsearchException(errorMsgFor(prefixesWithWildcard), e);
        }
    }

    private String errorMsgFor(String[] prefixesWithWildcard) {
        return "Failed to add field alias " + FIELD_GL2_MESSAGE_ID + " for indices " + Arrays.toString(prefixesWithWildcard) + ".";
    }

    static LinkedHashMap<String, Object> aliasMapping() {
        LinkedHashMap<String, Object> aliasMapping = new LinkedHashMap<>();
        aliasMapping.put("type", "alias");
        aliasMapping.put("path", "id");
        return aliasMapping;
    }
}
