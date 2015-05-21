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
import org.graylog2.restclient.models.Input;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;

public class InputDescription {
    @JsonIgnore
    private final Input input;

    public InputDescription(Input input) {
        this.input = checkNotNull(input);
    }

    @JsonProperty
    public String getId() {
        return input.getId();
    }

    @JsonProperty
    public String getName() {
        return input.getName();
    }

    @JsonProperty
    public String getTitle() {
        return input.getTitle();
    }

    @JsonProperty
    public String getCreatorUser() {
        return input.getCreatorUser() == null ? null : input.getCreatorUser().getName();
    }

    @JsonProperty
    public Boolean getGlobal() {
        return input.getGlobal();
    }

    @JsonProperty
    public DateTime getCreatedAt() {
        return input.getCreatedAt();
    }

    @JsonProperty
    public String getType() {
        return input.getType();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InputDescription that = (InputDescription) o;

        return input.equals(that.input);

    }

    @Override
    public int hashCode() {
        return input.hashCode();
    }
}
