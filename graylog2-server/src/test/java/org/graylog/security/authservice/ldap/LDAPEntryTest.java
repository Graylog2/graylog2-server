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
package org.graylog.security.authservice.ldap;

import com.google.common.collect.ImmutableList;
import com.unboundid.util.Base64;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LDAPEntryTest {
    @Test
    void dn() {
        final LDAPEntry entry = LDAPEntry.builder()
                .dn("cn=jane,ou=people,dc=example,dc=com")
                .base64UniqueId(Base64.encode("unique-id"))
                .addAttribute("foo", "bar")
                .build();

        assertThat(entry.dn()).isEqualTo("cn=jane,ou=people,dc=example,dc=com");
    }

    @Test
    void base64UniqueId() {
        final LDAPEntry entry = LDAPEntry.builder()
                .dn("cn=jane,ou=people,dc=example,dc=com")
                .base64UniqueId(Base64.encode("unique-id"))
                .addAttribute("foo", "bar")
                .build();

        assertThat(entry.base64UniqueId()).isEqualTo("dW5pcXVlLWlk");
    }

    @Test
    void objectClasses() {
        final LDAPEntry entry = LDAPEntry.builder()
                .dn("cn=jane,ou=people,dc=example,dc=com")
                .base64UniqueId(Base64.encode("unique-id"))
                .addAttribute("foo", "bar")
                .build();

        assertThat(entry.objectClasses()).isEmpty();

        final LDAPEntry entry2 = LDAPEntry.builder()
                .dn("cn=jane,ou=people,dc=example,dc=com")
                .base64UniqueId(Base64.encode("unique-id"))
                .addAttribute("foo", "bar")
                .objectClasses(Collections.singleton("inetOrgPerson"))
                .build();

        assertThat(entry2.objectClasses()).containsExactlyInAnyOrder("inetOrgPerson");
    }

    @Test
    void attributes() {
        final LDAPEntry entry = LDAPEntry.builder()
                .dn("cn=jane,ou=people,dc=example,dc=com")
                .base64UniqueId(Base64.encode("unique-id"))
                .addAttribute("foo", "bar")
                .build();

        assertThat(entry.attributes().get("foo")).containsExactly("bar");
        assertThat(entry.attributes().get("hello")).isEmpty();
    }

    @Test
    void addAndReadAttributes() {
        final LDAPEntry entry = LDAPEntry.builder()
                .dn("cn=jane,ou=people,dc=example,dc=com")
                .base64UniqueId(Base64.encode("unique-id"))
                .addAttribute("HeLLo", null)
                .addAttribute("Test", "1")
                .addAttribute("foo", "bar")
                .build();

        assertThat(entry.firstAttributeValue("hello")).isNotPresent();
        assertThat(entry.firstAttributeValue("test")).get().isEqualTo("1");
        assertThat(entry.firstAttributeValue("TEST")).get().isEqualTo("1");
        assertThat(entry.firstAttributeValue("foo")).get().isEqualTo("bar");
    }

    @Test
    void readNonBlankAttribute() {
        final LDAPEntry entry = LDAPEntry.builder()
                .dn("cn=jane,ou=people,dc=example,dc=com")
                .base64UniqueId(Base64.encode("unique-id"))
                .addAttribute("zero", "0")
                .addAttribute("one", null)
                .addAttribute("two", "")
                .addAttribute("three", "      ")
                .build();

        assertThat(entry.nonBlankAttribute("zero")).isEqualTo("0");
        assertThatThrownBy(() -> entry.nonBlankAttribute("one")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> entry.nonBlankAttribute("two")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> entry.nonBlankAttribute("three")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addAttributesWithDuplicateKey() {
        final LDAPEntry entry = LDAPEntry.builder()
                .dn("cn=jane,ou=people,dc=example,dc=com")
                .base64UniqueId(Base64.encode("unique-id"))
                .addAttribute("test", "1")
                .addAttribute("test", "2")
                .build();

        assertThat(entry.allAttributeValues("test")).get().isEqualTo(ImmutableList.of("1", "2"));
    }

    @Test
    void hasAttribute() {
        final LDAPEntry entry = LDAPEntry.builder()
                .dn("cn=jane,ou=people,dc=example,dc=com")
                .base64UniqueId(Base64.encode("unique-id"))
                .addAttribute("zero", "0")
                .addAttribute("one", null)
                .build();

        assertThat(entry.hasAttribute("zero")).isTrue();
        assertThat(entry.hasAttribute("ZERO")).isTrue();
        assertThat(entry.hasAttribute("one")).isFalse();
        assertThat(entry.hasAttribute("nope")).isFalse();
    }
}
