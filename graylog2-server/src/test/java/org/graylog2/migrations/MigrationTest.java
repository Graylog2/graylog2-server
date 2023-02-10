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

import com.google.common.collect.ImmutableSortedSet;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class MigrationTest {

    @Test
    public void testHashCode() {
        final int hashA = new MigrationA().hashCode();
        final int hashB = new MigrationB().hashCode();
        final int hashAa = new MigrationA().hashCode();

        assertThat(hashA).isNotEqualTo(hashB);
        assertThat(hashA).isEqualTo(hashAa);
    }

    @Test
    public void testEquals() {
        final MigrationA a = new MigrationA();
        final MigrationA aa = new MigrationA();
        final MigrationB b = new MigrationB();

        assertThat(a.equals(aa)).isTrue();
        assertThat(a.equals(b)).isFalse();
    }

    @Test
    public void testCompareTo() {
        final MigrationA a = new MigrationA();
        final MigrationA aa = new MigrationA();
        final MigrationB b = new MigrationB(); // same timestamp as A
        final MigrationC c = new MigrationC(); // oldest timestamp

        assertThat(a.compareTo(b)).isLessThan(0);
        assertThat(a.compareTo(aa)).isEqualTo(0);
        assertThat(a.compareTo(c)).isGreaterThan(1);

        final List<Migration> sorted = Stream.of(c, b, a).sorted().toList();
        assertThat(sorted).containsExactly(c, a, b);

        final ImmutableSortedSet<Migration> set = ImmutableSortedSet.of(a, aa, b, c);
        assertThat(set).containsExactly(c, a, b);
    }

    static class MigrationA extends Migration {
        @Override
        public ZonedDateTime createdAt() {
            return ZonedDateTime.parse("2016-11-16T17:21:00Z");
        }
        @Override
        public void upgrade() {
        }
    }

    static class MigrationB extends Migration {
        @Override
        public ZonedDateTime createdAt() {
            return ZonedDateTime.parse("2016-11-16T17:21:00Z");
        }
        @Override
        public void upgrade() {
        }
    }

    static class MigrationC extends Migration {
        @Override
        public ZonedDateTime createdAt() {
            return ZonedDateTime.parse("2014-11-16T17:21:00Z");
        }
        @Override
        public void upgrade() {
        }
    }
}
