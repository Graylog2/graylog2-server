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
package org.graylog2.migrations;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Objects;

public abstract class Migration implements Comparable<Migration> {
    private static final Comparator<Migration> COMPARATOR = Comparator.comparingLong(migration -> migration.createdAt().toEpochSecond());

    public abstract ZonedDateTime createdAt();

    public abstract void upgrade();

    @Override
    public int compareTo(Migration that) {
        return COMPARATOR.compare(this, that);
    }

    @Override
    public int hashCode() {
        return Objects.hash(COMPARATOR);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Migration that = (Migration) o;
        return Objects.equals(this.createdAt(), that.createdAt());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + '{' + createdAt().format(DateTimeFormatter.ISO_DATE_TIME) + '}';
    }
}
