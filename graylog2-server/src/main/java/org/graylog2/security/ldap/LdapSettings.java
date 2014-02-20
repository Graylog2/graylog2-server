/*
 * Copyright 2013-2014 TORCH GmbH
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
package org.graylog2.security.ldap;

import com.google.common.collect.Maps;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.apache.shiro.codec.Hex;
import org.bson.types.ObjectId;
import org.graylog2.Core;
import org.graylog2.database.Persisted;
import org.graylog2.database.validators.Validator;
import org.graylog2.security.AESTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

public class LdapSettings extends Persisted {
    private static final Logger log = LoggerFactory.getLogger(LdapSettings.class);

    private static final String COLLECTION = "ldap_settings";
    public static final String ENABLED = "enabled";
    public static final String SYSTEM_USERNAME = "system_username";
    public static final String SYSTEM_PASSWORD = "system_password";
    public static final String SYSTEM_PASSWORD_SALT = "system_password_salt";
    public static final String LDAP_URI = "ldap_uri";
    public static final String SEARCH_PATTERN = "principal_search_pattern";
    public static final String SEARCH_BASE = "search_base";
    public static final String DISPLAY_NAME_ATTRIBUTE = "username_attribute";
    public static final String USE_START_TLS = "use_start_tls";
    public static final String ACTIVE_DIRECTORY = "active_directory";
    public static final String DEFAULT_GROUP = "reader";
    public static final String TRUST_ALL_CERTS = "trust_all_certificates";

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
            log.error(
                    "Graylog2 does not yet support multiple LDAP backends, but {} configurations were found. This is a bug, ignoring LDAP config.",
                    results.size());
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
        if (o == null) return "";
        if (getSystemPasswordSalt().isEmpty()) {
            // this is an old version of the database that doesn't have the salt value,
            // simply return the password, because it's unencrypted.
            // The next time we will generate a salt and then re-use that value.
            // TODO remove this after 0.20 is out, and the RC versions are pulled.
            log.debug("Old database version does not have salted, encrypted password. Please save the LDAP settings again.");
            return o.toString();
        }
        String encryptedPw = o.toString();
        return AESTools.decrypt(
                encryptedPw,
                core.getConfiguration().getPasswordSecret().substring(0, 16),
                getSystemPasswordSalt());
    }

    public void setSystemPassword(String systemPassword) {
        // set new salt value, if we didn't have any.
        if (getSystemPasswordSalt().isEmpty()) {
            log.debug("Generating new salt for LDAP system password.");
            final SecureRandom random = new SecureRandom();
            byte[] saltBytes = new byte[8];
            random.nextBytes(saltBytes);
            setSystemPasswordSalt(Hex.encodeToString(saltBytes));
        }
        final String encrypted = AESTools.encrypt(
                systemPassword,
                core.getConfiguration().getPasswordSecret().substring(0, 16),
                getSystemPasswordSalt());
        fields.put(SYSTEM_PASSWORD, encrypted);
    }

    public String getSystemPasswordSalt() {
        Object o = fields.get(SYSTEM_PASSWORD_SALT);
        return (o!= null) ? o.toString() : "";
    }

    public void setSystemPasswordSalt(String salt) {
        fields.put(SYSTEM_PASSWORD_SALT, salt);
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

    public String getSearchPattern() {
        final Object o = fields.get(SEARCH_PATTERN);
        return o != null ? o.toString() : "";
    }

    public void setSearchPattern(String searchPattern) {
        fields.put(SEARCH_PATTERN, searchPattern);
    }

    public String getDisplayNameAttribute() {
        final Object o = fields.get(DISPLAY_NAME_ATTRIBUTE);
        return o != null ? o.toString() : "";
    }

    public void setDisplayNameAttribute(String displayNameAttribute) {
        fields.put(DISPLAY_NAME_ATTRIBUTE, displayNameAttribute);
    }

    public boolean isEnabled() {
        final Object o = fields.get(ENABLED);
        return o != null ? Boolean.valueOf(o.toString()) : false;
    }

    public void setEnabled(boolean enabled) {
        fields.put(ENABLED, enabled);
    }

    public void setUseStartTls(boolean useStartTls) {
        fields.put(USE_START_TLS, useStartTls);
    }

    public boolean isUseStartTls() {
        final Object o = fields.get(USE_START_TLS);
        return o != null ? Boolean.valueOf(o.toString()) : false;
    }

    public void setActiveDirectory(boolean activeDirectory) {
        fields.put(ACTIVE_DIRECTORY, activeDirectory);
    }

    public boolean isActiveDirectory() {
        final Object o = fields.get(ACTIVE_DIRECTORY);
        return o != null ? Boolean.valueOf(o.toString()) : false;
    }

    public String getDefaultGroup() {
        final Object o = fields.get(DEFAULT_GROUP);
        return o != null ? o.toString() : "reader"; // reader is the safe default
    }

    public void setDefaultGroup(String defaultGroup) {
        fields.put(DEFAULT_GROUP, defaultGroup);
    }

    public boolean isTrustAllCertificates() {
        final Object o = fields.get(TRUST_ALL_CERTS);
        return o != null ? Boolean.valueOf(o.toString()) : false;
    }

    public void setTrustAllCertificates(boolean trustAllCertificates) {
        fields.put(TRUST_ALL_CERTS, trustAllCertificates);
    }

}
