package org.graylog2.plugin.decorators;

import org.graylog2.rest.resources.search.responses.SearchResponse;

import java.util.function.Function;

@FunctionalInterface
public interface SearchResponseDecorator extends Function<SearchResponse, SearchResponse> {
}
