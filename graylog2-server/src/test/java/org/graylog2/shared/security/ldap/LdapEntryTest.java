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
package org.graylog2.shared.security.ldap;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LdapEntryTest {
    @Test
    void testEquals() {
        EqualsVerifier.forClass(LdapEntry.class)
                .suppress(Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    void caseInsensitiveAccess() {
        final LdapEntry entry = new LdapEntry();

        entry.put("HeLlo", "yo");

        assertThat(entry.get("HELLO")).isEqualTo("yo");
    }

    @Test
    void getEmail() {
        final LdapEntry entry = new LdapEntry();

        entry.put("mail", null);
        entry.put("rfc822Mailbox", "rfc-test@example.com");

        assertThat(entry.getEmail()).isEqualTo("rfc-test@example.com");

        entry.put("mail", "mail@example.com");

        assertThat(entry.getEmail()).isEqualTo("mail@example.com");
    }

    @Test
    void getNonBlank() {
        final LdapEntry entry = new LdapEntry();

        entry.put("null", null);
        entry.put("empty", "");
        entry.put("blank", "     ");
        entry.put("full", "hello");

        assertThatThrownBy(() -> entry.getNonBlank("null"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("null");
        assertThatThrownBy(() -> entry.getNonBlank("empty"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("null");
        assertThatThrownBy(() -> entry.getNonBlank("blank"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("null");

        assertThat(entry.getNonBlank("full")).isEqualTo("hello");
    }
}
