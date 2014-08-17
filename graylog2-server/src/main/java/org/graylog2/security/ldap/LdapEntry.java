/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.security.ldap;

import com.google.common.collect.Maps;

import java.util.Map;

public class LdapEntry {

    private Map<String, String> attributes = Maps.newHashMap();
    private String dn;

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getDn() {
        return dn;
    }

    public String get(String key) {
        return attributes.get(key.toLowerCase());
    }

    public String put(String key, String value) {
        return attributes.put(key.toLowerCase(), value);
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
}
