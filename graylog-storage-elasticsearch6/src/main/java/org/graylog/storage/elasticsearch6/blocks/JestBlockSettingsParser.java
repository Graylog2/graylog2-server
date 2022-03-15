package org.graylog.storage.elasticsearch6.blocks;

import com.fasterxml.jackson.databind.JsonNode;
import io.searchbox.client.JestResult;
import org.graylog2.indexer.indices.blocks.IndicesBlockStatus;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class JestBlockSettingsParser {

    static final Collection<String> POSSIBLE_BLOCKS = Arrays.asList("read_only", "read_only_allow_delete", "read", "write", "metadata");
    static final String BLOCK_SETTINGS_PREFIX = "index.blocks.";

    public IndicesBlockStatus parseBlockSettings(final JestResult jestResult, final List<String> indices) {
        final IndicesBlockStatus indicesBlockStatus = new IndicesBlockStatus();
        if (indices == null || indices.isEmpty() || jestResult == null) {
            return indicesBlockStatus;
        }
        final JsonNode jsonObject = jestResult.getJsonObject();
        if (jsonObject != null && !jsonObject.isMissingNode()) {
            for (String indexName : indices) {
                Collection<String> blocks = new LinkedList<>();
                for (String possibleBlock : POSSIBLE_BLOCKS) {
                    final JsonNode blockElem = jsonObject.path(indexName).path("settings").path("index").path("blocks").path(possibleBlock);
                    if (!blockElem.isMissingNode()) {
                        if ((blockElem.isBoolean() && blockElem.asBoolean()) ||
                                (blockElem.isTextual() && blockElem.asText().contains("true"))
                        ) {
                            blocks.add(BLOCK_SETTINGS_PREFIX + possibleBlock);
                        }
                    }
                }
                if (!blocks.isEmpty()) {
                    indicesBlockStatus.addIndexBlocks(indexName, blocks);
                }
            }
        }
        return indicesBlockStatus;
    }
}
