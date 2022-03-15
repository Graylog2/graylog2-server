package org.graylog2.indexer.indices.blocks;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class IndicesBlockStatus {

    private Map<String, Collection<String>> blocksPerIndex = new HashMap<>();

    public void addIndexBlocks(final String indexName, final Collection<String> blockTypes) {
        blocksPerIndex.put(indexName, blockTypes);
    }

    public Collection<String> getIndexBlocks(final String indexName) {
        return blocksPerIndex.get(indexName);
    }

    public int countBlockedIndices() {
        return blocksPerIndex.size();
    }

    public Set<String> getBlockedIndices() {
        return blocksPerIndex.keySet();
    }

    public List<String[]> toBlockDetails() {
        return blocksPerIndex.entrySet()
                .stream()
                .map(entry -> new String[]{entry.getKey(), String.join(", ", entry.getValue())})
                .collect(Collectors.toList());
    }
}
