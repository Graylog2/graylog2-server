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

import static org.graylog.plugins.views.search.export.LinkedHashSetUtil.linkedHashSetOf;

@AutoValue
public abstract class SimpleMessageChunk {
    public static SimpleMessageChunk from(LinkedHashSet<String> fieldsInOrder, LinkedHashSet<SimpleMessage> messages) {
        return builder().fieldsInOrder(fieldsInOrder).messages(messages).build();
    }

    public static SimpleMessageChunk from(LinkedHashSet<String> fieldsInOrder, SimpleMessage... messages) {
        return from(fieldsInOrder, linkedHashSetOf(messages));
    }

    public abstract LinkedHashSet<String> fieldsInOrder();

    public abstract LinkedHashSet<SimpleMessage> messages();

    public abstract boolean isFirstChunk();

    public int size() {
        return messages().size();
    }

    public static Builder builder() {
        return Builder.create().isFirstChunk(false);
    }

    public abstract Builder toBuilder();

    public Object[][] getAllValuesInOrder() {
        return messages().stream()
                .map(this::valuesFrom)
                .toArray(Object[][]::new);
    }

    private Object[] valuesFrom(SimpleMessage simpleMessage) {
        return fieldsInOrder().stream().map(simpleMessage::valueFor).toArray();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder fieldsInOrder(LinkedHashSet<String> fieldsInOrder);

        public abstract Builder messages(LinkedHashSet<SimpleMessage> messages);

        public abstract Builder isFirstChunk(boolean isFirstChunk);

        public static Builder create() {
            return new AutoValue_SimpleMessageChunk.Builder();
        }

        abstract SimpleMessageChunk autoBuild();

        public SimpleMessageChunk build() {
            return autoBuild();
        }
    }
}
