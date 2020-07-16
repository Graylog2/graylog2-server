package org.graylog.storage.elasticsearch7;

import com.google.auto.value.AutoValue;
import org.graylog2.indexer.searches.ScrollCommand;
import org.graylog2.indexer.searches.SearchesConfig;
import org.graylog2.indexer.searches.Sorting;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;

@AutoValue
abstract class SearchCommand {
    public abstract String query();
    public abstract Optional<Set<String>> streams();
    public abstract Optional<Sorting> sorting();
    public abstract Optional<String> filter();
    public abstract Optional<TimeRange> range();
    public abstract OptionalInt limit();
    public abstract OptionalInt offset();
    public abstract OptionalLong batchSize();
    public abstract boolean highlight();

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static SearchCommand create(
            String query,
            Optional<Set<String>> streams,
            Optional<Sorting> sorting,
            Optional<String> filter,
            Optional<TimeRange> range,
            OptionalInt limit,
            OptionalInt offset,
            OptionalLong batchSize,
            boolean highlight) {
        return new AutoValue_SearchCommand(query, streams, sorting, filter, range, limit, offset, batchSize, highlight);
    }

    static SearchCommand from(SearchesConfig searchesConfig) {
        return create(searchesConfig.query(), Optional.empty(), Optional.ofNullable(searchesConfig.sorting()),
                Optional.ofNullable(searchesConfig.filter()), Optional.of(searchesConfig.range()),
                OptionalInt.of(searchesConfig.limit()), OptionalInt.of(searchesConfig.offset()),
                OptionalLong.empty(), true);
    }

    static SearchCommand from(ScrollCommand scrollCommand) {
        return create(scrollCommand.query(), scrollCommand.streams(), scrollCommand.sorting(),
                scrollCommand.filter(), scrollCommand.range(), scrollCommand.limit(), scrollCommand.offset(),
                scrollCommand.batchSize(), scrollCommand.highlight());
    }
}
