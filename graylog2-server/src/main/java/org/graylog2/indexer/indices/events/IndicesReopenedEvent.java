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
package org.graylog2.indexer.indices.events;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.graylog.autovalue.WithBeanGetter;

import java.util.Set;

@AutoValue
@WithBeanGetter
public abstract class IndicesReopenedEvent {
    public abstract Set<String> indices();

    public static IndicesReopenedEvent create(Set<String> indices) {
        return new AutoValue_IndicesReopenedEvent(ImmutableSet.copyOf(indices));
    }

    public static IndicesReopenedEvent create(String index) {
        return new AutoValue_IndicesReopenedEvent(ImmutableSet.of(index));
    }
}
