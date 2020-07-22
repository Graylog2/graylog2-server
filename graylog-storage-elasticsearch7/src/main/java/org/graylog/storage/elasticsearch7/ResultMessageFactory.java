package org.graylog.storage.elasticsearch7;

import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.text.Text;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.SearchHit;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.graylog2.indexer.results.ResultMessage;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ResultMessageFactory {
    public static ResultMessage fromSearchHit(SearchHit hit) {
        final Map<String, List<String>> highlights = hit.getHighlightFields().entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, ResultMessageFactory::highlightsFromFragments));
        return ResultMessage.parseFromSource(hit.getId(), hit.getIndex(), hit.getSourceAsMap(), highlights);
    }

    private static List<String> highlightsFromFragments(Map.Entry<String, HighlightField> entry) {
        return Arrays.stream(entry.getValue().fragments())
                .map(Text::toString)
                .collect(Collectors.toList());
    }
}
