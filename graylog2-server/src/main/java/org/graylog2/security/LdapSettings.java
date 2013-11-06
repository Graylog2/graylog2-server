/*
 * Copyright 2013 TORCH UG
 *
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
package org.graylog2.security;

import com.google.common.collect.Maps;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.Core;
import org.graylog2.database.Persisted;
import org.graylog2.database.validators.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Map;

public class LdapSettings extends Persisted {
    private static final Logger log = LoggerFactory.getLogger(LdapSettings.class);

    private static final String COLLECTION = "ldap_settings";
    public static final String ENABLED = "enabled";
    public static final String SYSTEM_USERNAME = "system_username";
    public static final String SYSTEM_PASSWORD = "system_password";
    public static final String LDAP_URI = "ldap_uri";
    public static final String PRINCIPAL_SEARCH_PATTERN = "principal_search_pattern";
    public static final String SEARCH_BASE = "search_base";
    public static final String USERNAME_ATTRIBUTE = "username_attribute";

    public LdapSettings(Core core) {
        super(core, Maps.<String, Object>newHashMap());
    }

    protected LdapSettings(Core core, ObjectId id, Map<String, Object> fields) {
        super(core, id, fields);
    }

    public static LdapSettings load(Core core) {
        DBObject query = new BasicDBObject();
        final List<DBObject> results = query(query, core, COLLECTION);
        if (results.size() == 0) {
            return null;
        }
        if (results.size() > 1) {
            log.error("Graylog2 does not yet support multiple LDAP backends, but {} configurations were found. This is a bug, ignoring LDAP config.", results.size());
            return null;
        }
        final DBObject settingsObject = results.get(0);
        return new LdapSettings(core, (ObjectId) settingsObject.get("_id"), settingsObject.toMap());
    }

    public static void delete(Core core) {
        DBObject query = new BasicDBObject();
        destroy(query, core, COLLECTION);
    }


    @Override
    public String getCollectionName() {
        return COLLECTION;
    }

    @Override
    protected Map<String, Validator> getValidations() {
        return null;
    }

    @Override
    protected Map<String, Validator> getEmbeddedValidations(String key) {
        return null;
    }

    public String getSystemUserName() {
        final Object o = fields.get(SYSTEM_USERNAME);
        return o != null ? o.toString() : "";
    }

    public void setSystemUsername(String systemUsername) {
        fields.put(SYSTEM_USERNAME, systemUsername);
    }

    public String getSystemPassword() {
        final Object o = fields.get(SYSTEM_PASSWORD);
        return o != null ? o.toString() : "";
    }

    public void setSystemPassword(String systemPassword) {
        fields.put(SYSTEM_PASSWORD, systemPassword);
    }

    public URI getUri() {
        final Object o = fields.get(LDAP_URI);
        return o != null ? URI.create(o.toString()) : null;
    }

    public void setUri(URI ldapUri) {
        fields.put(LDAP_URI, ldapUri.toString());
    }

    public String getSearchBase() {
        final Object o = fields.get(SEARCH_BASE);
        return o != null ? o.toString() : "";
    }

    public void setSearchBase(String searchBase) {
        fields.put(SEARCH_BASE, searchBase);
    }

    public String getPrincipalSearchPattern() {
        final Object o = fields.get(PRINCIPAL_SEARCH_PATTERN);
        return o != null ? o.toString() : "";
    }

    public void setPrincipalSearchPattern(String principalSearchPattern) {
        fields.put(PRINCIPAL_SEARCH_PATTERN, principalSearchPattern);
    }

    public String getUsernameAttribute() {
        final Object o = fields.get(USERNAME_ATTRIBUTE);
        return o != null ? o.toString() : "";
    }

    public void setUsernameAttribute(String usernameAttribute) {
        fields.put(USERNAME_ATTRIBUTE, usernameAttribute);
    }

    public boolean isEnabled() {
        final Object o = fields.get(ENABLED);
        return o != null ? Boolean.valueOf(o.toString()) : false;
    }

    public void setEnabled(boolean enabled) {
        fields.put(ENABLED, enabled);
    }
}
