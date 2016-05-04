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
package org.graylog2.security.ldap;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PersistedServiceImpl;
import org.graylog2.shared.security.ldap.LdapSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;

public class LdapSettingsServiceImpl extends PersistedServiceImpl implements LdapSettingsService {
    private static final Logger LOG = LoggerFactory.getLogger(LdapSettingsServiceImpl.class);
    private final LdapSettingsImpl.Factory ldapSettingsFactory;

    @Inject
    public LdapSettingsServiceImpl(MongoConnection mongoConnection, LdapSettingsImpl.Factory ldapSettingsFactory) {
        super(mongoConnection);
        this.ldapSettingsFactory = ldapSettingsFactory;
    }

    @Override
    @Nullable
    public LdapSettings load() {
        final List<DBObject> results = query(LdapSettingsImpl.class, new BasicDBObject());
        if (results.size() == 0) {
            return null;
        }
        if (results.size() > 1) {
            LOG.error(
                    "Graylog does not yet support multiple LDAP backends, but {} configurations were found. This is a bug, ignoring LDAP config.",
                    results.size());
            return null;
        }
        final DBObject settings = results.get(0);
        final LdapSettingsImpl ldapSettings = ldapSettingsFactory.create((ObjectId) settings.get("_id"), settings.toMap());
        if (null == ldapSettings.getSystemPassword()) {
            return null;
        }
        return ldapSettings;
    }

    @Override
    public void delete() {
        destroyAll(LdapSettingsImpl.class);
    }
}
