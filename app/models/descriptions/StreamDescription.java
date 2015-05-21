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

package models.descriptions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.restclient.models.Stream;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;

public class StreamDescription {
    @JsonIgnore
    private final Stream stream;

    private StreamDescription(Stream stream) {
        this.stream = checkNotNull(stream);
    }

    public static StreamDescription of(Stream stream) {
        if (stream == null) {
            return null;
        }
        return new StreamDescription(stream);
    }

    @JsonProperty
    public String getDescription() {
        return stream.getDescription();
    }

    @JsonProperty
    public String getTitle() {
        return stream.getTitle();
    }

    @JsonProperty
    public String getCreatorUser() {
        return stream.getCreatorUser() == null ? null : stream.getCreatorUser().getName();
    }

    @JsonProperty
    public DateTime getCreatedAt() {
        return stream.getCreatedAt();
    }

    @JsonProperty
    public String getId() {
        return stream.getId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StreamDescription that = (StreamDescription) o;

        return stream.equals(that.stream);

    }

    @Override
    public int hashCode() {
        return stream.hashCode();
    }
}
