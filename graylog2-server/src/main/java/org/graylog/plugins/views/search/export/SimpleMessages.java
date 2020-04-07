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
package org.graylog.plugins.views.search.export;

import com.google.auto.value.AutoValue;

import java.util.LinkedHashSet;

@AutoValue
public abstract class SimpleMessages {
    public static SimpleMessages from(LinkedHashSet<SimpleMessage> messages) {
        return builder().messages(messages).build();
    }

    public abstract LinkedHashSet<SimpleMessage> messages();

    public int size() {
        return messages().size();
    }

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder messages(LinkedHashSet<SimpleMessage> messages);

        public static Builder create() {
            return new AutoValue_SimpleMessages.Builder();
        }

        abstract SimpleMessages autoBuild();

        public SimpleMessages build() {
            return autoBuild();
        }
    }
}
