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

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PersistedServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public LdapSettings load() {
        DBObject query = new BasicDBObject();
        final List<DBObject> results = query(LdapSettingsImpl.class, query);
        if (results.size() == 0) {
            return null;
        }
        if (results.size() > 1) {
            LOG.error(
                    "Graylog2 does not yet support multiple LDAP backends, but {} configurations were found. This is a bug, ignoring LDAP config.",
                    results.size());
            return null;
        }
        final DBObject settingsObject = results.get(0);
        return ldapSettingsFactory.create((ObjectId) settingsObject.get("_id"), settingsObject.toMap());
    }

    @Override
    public void delete() {
        DBObject query = new BasicDBObject();
        destroyAll(LdapSettingsImpl.class, query);
    }
}