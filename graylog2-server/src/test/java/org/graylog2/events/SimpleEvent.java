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
package org.graylog2.events;

import java.util.Objects;

public class SimpleEvent {
    public String payload;

    public SimpleEvent() {}

    public SimpleEvent(String payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "payload=" + payload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SimpleEvent event = (SimpleEvent) o;
        return Objects.equals(payload, event.payload);
    }

    @Override
    public int hashCode() {
        return payload != null ? payload.hashCode() : 0;
    }
}