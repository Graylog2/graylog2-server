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
package org.graylog2.audit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class AuditEventTypeTest {
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testStringType() throws Exception {
        final AuditEventType type = AuditEventType.create("namespace:object:action");

        assertThat(type.namespace()).isEqualTo("namespace");
        assertThat(type.object()).isEqualTo("object");
        assertThat(type.action()).isEqualTo("action");
    }

    @Test
    public void testStringTypeWithMoreColons() throws Exception {
        final AuditEventType type = AuditEventType.create("namespace:object:action:foo");

        assertThat(type.namespace()).isEqualTo("namespace");
        assertThat(type.object()).isEqualTo("object");
        assertThat(type.action()).isEqualTo("action:foo");
    }

    @Test
    public void testToTypeString() throws Exception {
        final AuditEventType type = AuditEventType.create("namespace:object:action");

        assertThat(type.toTypeString()).isEqualTo("namespace:object:action");
    }

    @Test
    public void testInvalid1() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        AuditEventType.create("foo");
    }

    @Test
    public void testInvalid2() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        AuditEventType.create("");
    }

    @Test
    public void testInvalid3() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        AuditEventType.create(null);
    }
}