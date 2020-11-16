/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
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
