/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.decorators;

import com.google.common.collect.Sets;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.decorators.SearchResponseDecorator;
import org.graylog2.rest.models.messages.responses.DecorationStats;
import org.graylog2.rest.models.messages.responses.ResultMessageSummary;
import org.graylog2.rest.resources.search.responses.SearchDecorationStats;
import org.graylog2.rest.resources.search.responses.SearchResponse;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DecoratorProcessorImpl implements DecoratorProcessor {
    private final DecoratorResolver decoratorResolver;

    @Inject
    public DecoratorProcessorImpl(DecoratorResolver decoratorResolver) {
        this.decoratorResolver = decoratorResolver;
    }

    @Override
    public SearchResponse decorate(SearchResponse searchResponse, Optional<String> streamId) {
        final List<SearchResponseDecorator> searchResponseDecorators = streamId.isPresent() ?
            decoratorResolver.searchResponseDecoratorsForStream(streamId.get()) : decoratorResolver.searchResponseDecoratorsForGlobal();
        final Optional<SearchResponseDecorator> metaDecorator = searchResponseDecorators.stream()
            .reduce((f, g) -> (v) -> g.apply(f.apply(v)));
        if (metaDecorator.isPresent()) {
            final Map<String, ResultMessageSummary> originalMessages = searchResponse.messages()
                .stream()
                .collect(Collectors.toMap(message -> message.message().get("_id").toString(), Function.identity()));
            final SearchResponse newSearchResponse = metaDecorator.get().apply(searchResponse);
            final Set<String> newFields = extractFields(newSearchResponse.messages());
            final Set<String> addedFields = Sets.difference(newFields, searchResponse.fields())
                .stream()
                .filter(field -> !Message.RESERVED_FIELDS.contains(field) && !field.equals("streams"))
                .collect(Collectors.toSet());

            final List<ResultMessageSummary> decoratedMessages = newSearchResponse.messages()
                .stream()
                .map(resultMessage -> {
                    final ResultMessageSummary originalMessage = originalMessages.get(resultMessage.message().get("_id").toString());
                    if (originalMessage != null) {
                        return resultMessage
                            .toBuilder()
                            .decorationStats(DecorationStats.create(originalMessage.message(), resultMessage.message()))
                            .build();
                    }
                    return resultMessage;
                })
                .collect(Collectors.toList());
            return newSearchResponse
                .toBuilder()
                .messages(decoratedMessages)
                .fields(newFields)
                .decorationStats(SearchDecorationStats.create(addedFields))
                .build();
        }

        return searchResponse;
    }

    private Set<String> extractFields(List<ResultMessageSummary> messages) {
        return messages.stream()
            .map(message -> message.message().keySet())
            .reduce(new HashSet<>(), (set1, set2) -> { set1.addAll(set2); return set1; });
    }
}
