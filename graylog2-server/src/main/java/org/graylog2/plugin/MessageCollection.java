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
package org.graylog2.plugin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;

import java.util.Iterator;

public class MessageCollection implements Messages  {

    private final ImmutableList<Message> messages;

    public MessageCollection(Iterable<Message> other) {
        messages = ImmutableList.copyOf(other);
    }

    @Override
    public Iterator<Message> iterator() {
        return Iterators.filter(messages.iterator(), e -> !e.getFilterOut());
    }
}
