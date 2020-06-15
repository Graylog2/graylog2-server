package org.graylog2.indexer.searches;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.auto.value.AutoValue;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

@AutoValue
@JsonAutoDetect
public abstract class ScrollCommand {
    public abstract String query();
    public abstract Set<String> indices();
    public abstract Optional<Sorting> sorting();
    public abstract Optional<String> filter();
    public abstract Optional<TimeRange> range();
    public abstract OptionalInt limit();
    public abstract OptionalInt offset();
    public abstract List<String> fields();
    public abstract OptionalInt batchSize();
    public abstract boolean highlight();

    public static Builder builder() {
        return new AutoValue_ScrollCommand.Builder()
                .highlight(false);
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder query(String query);
        public abstract Builder indices(Set<String> indices);
        public abstract Builder sorting(Sorting sorting);
        public abstract Builder filter(String filter);
        public abstract Builder range(TimeRange range);
        public abstract Builder limit(int limit);
        public abstract Builder offset(int offset);
        public abstract Builder fields(List<String> fields);
        public abstract Builder batchSize(int batchSize);
        public abstract Builder highlight(boolean highlight);

        public abstract ScrollCommand build();
    }
}
