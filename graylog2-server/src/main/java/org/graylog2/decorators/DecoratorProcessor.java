package org.graylog2.decorators;

import com.google.inject.ImplementedBy;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.rest.resources.search.responses.SearchResponse;

import java.util.List;
import java.util.Optional;

@ImplementedBy(DecoratorProcessorImpl.class)
public interface DecoratorProcessor {
    List<ResultMessage> decorate(List<ResultMessage> messages, Optional<String> stream);
    SearchResponse decorate(SearchResponse searchResponse, Optional<String> stream);
}
