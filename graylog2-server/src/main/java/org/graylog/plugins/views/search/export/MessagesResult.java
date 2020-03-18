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

@AutoValue
public abstract class MessagesResult {
    public abstract String filename();

    public abstract String fileContents();

    public static MessagesResult.Builder builder() {
        return new AutoValue_MessagesResult.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract MessagesResult.Builder filename(String filename);

        public abstract MessagesResult.Builder fileContents(String fileContents);

        abstract MessagesResult autoBuild();

        public MessagesResult build() {
            return autoBuild();
        }
    }
}
