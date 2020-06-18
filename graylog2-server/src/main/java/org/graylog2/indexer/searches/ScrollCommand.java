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
package org.graylog2.indexer.searches;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.auto.value.AutoValue;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;

@AutoValue
@JsonAutoDetect
public abstract class ScrollCommand {
    public abstract String query();
    public abstract Set<String> indices();
    public abstract Optional<Set<String>> streams();
    public abstract Optional<Sorting> sorting();
    public abstract Optional<String> filter();
    public abstract Optional<TimeRange> range();
    public abstract OptionalInt limit();
    public abstract OptionalInt offset();
    public abstract List<String> fields();
    public abstract OptionalLong batchSize();
    public abstract boolean highlight();

    public static Builder builder() {
        return new AutoValue_ScrollCommand.Builder()
                .query("")
                .fields(Collections.emptyList())
                .highlight(false);
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder query(String query);
        public abstract Builder indices(Set<String> indices);
        public abstract Builder streams(Set<String> streams);
        public abstract Builder sorting(Sorting sorting);
        public abstract Builder filter(@Nullable String filter);
        public abstract Builder range(TimeRange range);
        public abstract Builder limit(int limit);
        public abstract Builder offset(int offset);
        public abstract Builder fields(List<String> fields);
        public abstract Builder batchSize(int batchSize);
        public abstract Builder highlight(boolean highlight);

        public abstract ScrollCommand build();
    }
}
