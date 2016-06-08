package org.graylog2.decorators;

import com.google.inject.ImplementedBy;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.rest.resources.search.responses.SearchResponse;

import java.util.List;

@ImplementedBy(DecoratorProcessorImpl.class)
public interface DecoratorProcessor {
    List<ResultMessage> decorate(List<ResultMessage> messages);
    SearchResponse decorate(SearchResponse searchResponse);
}
