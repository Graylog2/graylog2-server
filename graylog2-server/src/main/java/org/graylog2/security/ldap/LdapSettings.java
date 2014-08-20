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

import org.graylog2.plugin.database.Persisted;

import java.net.URI;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public interface LdapSettings extends Persisted {
    String getSystemUserName();

    void setSystemUsername(String systemUsername);

    String getSystemPassword();

    void setSystemPassword(String systemPassword);

    String getSystemPasswordSalt();

    void setSystemPasswordSalt(String salt);

    URI getUri();

    void setUri(URI ldapUri);

    String getSearchBase();

    void setSearchBase(String searchBase);

    String getSearchPattern();

    void setSearchPattern(String searchPattern);

    String getDisplayNameAttribute();

    void setDisplayNameAttribute(String displayNameAttribute);

    boolean isEnabled();

    void setEnabled(boolean enabled);

    void setUseStartTls(boolean useStartTls);

    boolean isUseStartTls();

    void setActiveDirectory(boolean activeDirectory);

    boolean isActiveDirectory();

    String getDefaultGroup();

    void setDefaultGroup(String defaultGroup);

    boolean isTrustAllCertificates();

    void setTrustAllCertificates(boolean trustAllCertificates);
}
