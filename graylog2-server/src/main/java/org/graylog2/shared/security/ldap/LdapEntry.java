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

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class LdapEntry {

    private Map<String, String> attributes = Maps.newHashMap();
    private Set<String> groups = Sets.newHashSet();
    private String dn;
    private String bindPrincipal;

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getDn() {
        return dn;
    }

    public String getBindPrincipal() {
        return bindPrincipal;
    }

    public void setBindPrincipal(String bindPrincipal) {
        this.bindPrincipal = bindPrincipal;
    }

    public String get(String key) {
        return attributes.get(key.toLowerCase(Locale.ENGLISH));
    }

    public String put(String key, String value) {
        return attributes.put(key.toLowerCase(Locale.ENGLISH), value);
    }

    public void addGroups(Collection<String> groups) {
        this.groups.addAll(groups);
    }

    public Set<String> getGroups() {
        return groups;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public String getEmail() {
        String email = get("mail");
        if (email == null) {
            email = get("rfc822Mailbox");
        }
        return email;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("dn", dn)
                .add("bindPrincipal", bindPrincipal)
                .add("attributes", attributes)
                .add("groups", groups)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LdapEntry ldapEntry = (LdapEntry) o;
        return Objects.equals(attributes, ldapEntry.attributes) &&
                Objects.equals(groups, ldapEntry.groups) &&
                Objects.equals(dn, ldapEntry.dn) &&
                Objects.equals(bindPrincipal, ldapEntry.bindPrincipal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributes, groups, dn, bindPrincipal);
    }
}
