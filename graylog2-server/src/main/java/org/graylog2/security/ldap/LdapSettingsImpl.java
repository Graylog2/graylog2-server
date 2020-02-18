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

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.mongodb.BasicDBList;
import com.mongodb.DBObject;
import org.apache.shiro.codec.Hex;
import org.bson.types.ObjectId;
import org.graylog2.Configuration;
import org.graylog2.database.CollectionName;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PersistedImpl;
import org.graylog2.plugin.database.validators.Validator;
import org.graylog2.security.AESTools;
import org.graylog2.shared.security.ldap.LdapSettings;
import org.graylog2.shared.users.Role;
import org.graylog2.shared.users.Roles;
import org.graylog2.users.RoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URI;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@CollectionName("ldap_settings")
public class LdapSettingsImpl extends PersistedImpl implements LdapSettings {


    public interface Factory {
        LdapSettingsImpl createEmpty();
        LdapSettingsImpl create(ObjectId objectId, Map<String, Object> fields);
    }

    private static final Logger LOG = LoggerFactory.getLogger(LdapSettingsImpl.class);

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
    public static final String DEFAULT_GROUP = "default_group";
    public static final String TRUST_ALL_CERTS = "trust_all_certificates";
    public static final String GROUP_MAPPING = "group_role_mapping";
    public static final String GROUP_MAPPING_LIST = "group_role_mapping_list";
    public static final String GROUP_SEARCH_BASE = "group_search_base";
    public static final String GROUP_ID_ATTRIBUTE = "group_id_attribute";
    public static final String GROUP_SEARCH_PATTERN = "group_search_pattern";
    public static final String ADDITIONAL_DEFAULT_GROUPS = "additional_default_groups";

    public static final String LDAP_GROUP_MAPPING_NAMEKEY = "group";
    public static final String LDAP_GROUP_MAPPING_ROLEKEY = "role_id";

    protected Configuration configuration;
    private final RoleService roleService;

    @AssistedInject
    public LdapSettingsImpl(Configuration configuration, RoleService roleService) {
        super(Maps.<String, Object>newHashMap());
        this.configuration = configuration;
        this.roleService = roleService;
    }

    @AssistedInject
    public LdapSettingsImpl(Configuration configuration, RoleService roleService, @Assisted ObjectId id, @Assisted Map<String, Object> fields) {
        super(id, fields);
        this.configuration = configuration;
        this.roleService = roleService;
    }

    @Override
    public Map<String, Validator> getValidations() {
        return null;
    }

    @Override
    public Map<String, Validator> getEmbeddedValidations(String key) {
        return null;
    }

    @Override
    public String getSystemUserName() {
        return Strings.nullToEmpty((String) fields.get(SYSTEM_USERNAME));
    }

    @Override
    public void setSystemUsername(String systemUsername) {
        fields.put(SYSTEM_USERNAME, systemUsername);
    }

    @Override
    public String getSystemPassword() {
        final Object o = fields.get(SYSTEM_PASSWORD);
        if (o == null) return "";
        if (getSystemPasswordSalt().isEmpty()) {
            // this is an old version of the database that doesn't have the salt value,
            // simply return the password, because it's unencrypted.
            // The next time we will generate a salt and then re-use that value.
            // TODO remove this after 0.20 is out, and the RC versions are pulled.
            LOG.debug("Old database version does not have salted, encrypted password. Please save the LDAP settings again.");
            return o.toString();
        }
        String encryptedPw = o.toString();
        return AESTools.decrypt(
                encryptedPw,
                configuration.getPasswordSecret().substring(0, 16),
                getSystemPasswordSalt());
    }

    @Override
    public boolean isSystemPasswordSet() {
        final Object o = fields.get(SYSTEM_PASSWORD);
        return o != null;
    }

    @Override
    public void setSystemPassword(String systemPassword) {
        if (systemPassword == null || systemPassword.isEmpty()) {
            return;
        }
        // set new salt value, if we didn't have any.
        if (getSystemPasswordSalt().isEmpty()) {
            LOG.debug("Generating new salt for LDAP system password.");
            final SecureRandom random = new SecureRandom();
            byte[] saltBytes = new byte[8];
            random.nextBytes(saltBytes);
            setSystemPasswordSalt(Hex.encodeToString(saltBytes));
        }
        final String encrypted = AESTools.encrypt(
                systemPassword,
                configuration.getPasswordSecret().substring(0, 16),
                getSystemPasswordSalt());
        fields.put(SYSTEM_PASSWORD, encrypted);
    }

    @Override
    public String getSystemPasswordSalt() {
        return Strings.nullToEmpty((String) fields.get(SYSTEM_PASSWORD_SALT));
    }

    @Override
    public void setSystemPasswordSalt(String salt) {
        fields.put(SYSTEM_PASSWORD_SALT, salt);
    }

    @Override
    public URI getUri() {
        final Object o = fields.get(LDAP_URI);
        return o != null ? URI.create(o.toString()) : null;
    }

    @Override
    public void setUri(URI ldapUri) {
        fields.put(LDAP_URI, ldapUri.toString());
    }

    @Override
    public String getSearchBase() {
        return Strings.nullToEmpty((String) fields.get(SEARCH_BASE));
    }

    @Override
    public void setSearchBase(String searchBase) {
        fields.put(SEARCH_BASE, searchBase);
    }

    @Override
    public String getSearchPattern() {
        return Strings.nullToEmpty((String) fields.get(SEARCH_PATTERN));
    }

    @Override
    public void setSearchPattern(String searchPattern) {
        fields.put(SEARCH_PATTERN, searchPattern);
    }

    @Override
    public String getDisplayNameAttribute() {
        return Strings.nullToEmpty((String) fields.get(DISPLAY_NAME_ATTRIBUTE));
    }

    @Override
    public void setDisplayNameAttribute(String displayNameAttribute) {
        fields.put(DISPLAY_NAME_ATTRIBUTE, displayNameAttribute);
    }

    @Override
    public boolean isEnabled() {
        final Object o = fields.get(ENABLED);
        return o != null ? Boolean.valueOf(o.toString()) : false;
    }

    @Override
    public void setEnabled(boolean enabled) {
        fields.put(ENABLED, enabled);
    }

    @Override
    public void setUseStartTls(boolean useStartTls) {
        fields.put(USE_START_TLS, useStartTls);
    }

    @Override
    public boolean isUseStartTls() {
        final Object o = fields.get(USE_START_TLS);
        return o != null ? Boolean.valueOf(o.toString()) : false;
    }

    @Override
    public void setActiveDirectory(boolean activeDirectory) {
        fields.put(ACTIVE_DIRECTORY, activeDirectory);
    }

    @Override
    public boolean isActiveDirectory() {
        final Object o = fields.get(ACTIVE_DIRECTORY);
        return o != null ? Boolean.valueOf(o.toString()) : false;
    }

    @Override
    public String getDefaultGroup() {
        final String defaultGroupId = getDefaultGroupId();
        if (defaultGroupId.equals(roleService.getReaderRoleObjectId())) {
            return "Reader";
        }
        try {
            final Map<String, Role> idToRole = roleService.loadAllIdMap();
            return idToRole.get(defaultGroupId).getName();
        } catch (Exception e) {
            LOG.error("Unable to load role mapping");
            return "Reader";
        }
    }

    @Override
    public String getDefaultGroupId() {
        final Object o = fields.get(DEFAULT_GROUP);
        return o == null ? roleService.getReaderRoleObjectId() : (String) o;
    }

    @Override
    public void setDefaultGroup(String defaultGroup) {
        String groupId = roleService.getReaderRoleObjectId();
        try {
            groupId = roleService.load(defaultGroup).getId();
        } catch (NotFoundException e) {
            LOG.error("Unable to load role mapping");
        }
        fields.put(DEFAULT_GROUP, groupId);
    }

    @Override
    public boolean isTrustAllCertificates() {
        final Object o = fields.get(TRUST_ALL_CERTS);
        return o != null ? Boolean.valueOf(o.toString()) : false;
    }

    @Override
    public void setTrustAllCertificates(boolean trustAllCertificates) {
        fields.put(TRUST_ALL_CERTS, trustAllCertificates);
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, String> getGroupMapping() {
        final BasicDBList groupMappingList = (BasicDBList) fields.get(GROUP_MAPPING_LIST);
        final Map<String, String> groupMapping;

        if (groupMappingList == null) {
            // pre-2.0 storage format, convert after read
            // should actually have been converted during start, but let's play safe here
            groupMapping = (Map<String, String>) fields.get(GROUP_MAPPING);
        } else {
            groupMapping = Maps.newHashMapWithExpectedSize(groupMappingList.size());
            for (Object entry : groupMappingList) {
                final DBObject field = (DBObject) entry;

                groupMapping.put((String) field.get(LDAP_GROUP_MAPPING_NAMEKEY),
                                 (String) field.get(LDAP_GROUP_MAPPING_ROLEKEY));
            }
        }
        if (groupMapping == null || groupMapping.isEmpty()) {
            return Collections.emptyMap();
        }
        else {
            // we store role ids, but the outside world uses role names to identify them
            try {
                final Map<String, Role> idToRole = roleService.loadAllIdMap();
                return Maps.newHashMap(Maps.transformValues(groupMapping, Roles.roleIdToNameFunction(idToRole)));
            } catch (NotFoundException e) {
                LOG.error("Unable to load role mapping");
                return Collections.emptyMap();
            }
        }
    }

    @Override
    public void setGroupMapping(Map<String, String> mapping) {
        Map<String, String> internal;
        if (mapping == null) {
            internal = Collections.emptyMap();
        } else {
            // we store ids internally but external users use the group names
            try {
                final Map<String, Role> nameToRole = Maps.uniqueIndex(roleService.loadAll(), Roles.roleToNameFunction());

                internal = Maps.newHashMap(Maps.transformValues(mapping, new Function<String, String>() {
                    @Nullable
                    @Override
                    public String apply(@Nullable String groupName) {
                        if (groupName == null || !nameToRole.containsKey(groupName)) {
                            return null;
                        }
                        return nameToRole.get(groupName).getId();
                    }
                }));
            } catch (NotFoundException e) {
                LOG.error("Unable to convert group names to ids", e);
                throw new IllegalStateException("Unable to convert group names to ids", e);
            }
        }

        // convert the group -> role_id map to a list of {"group" -> group, "role_id" -> roleid } maps
        fields.put(GROUP_MAPPING_LIST,
                   internal.entrySet().stream()
                           .map(entry -> {
                               Map<String, String> m = Maps.newHashMap();
                               m.put(LDAP_GROUP_MAPPING_NAMEKEY, entry.getKey());
                               m.put(LDAP_GROUP_MAPPING_ROLEKEY, entry.getValue());
                               return m;
                           })
                           .collect(Collectors.toList()));
    }

    @Override
    public String getGroupSearchBase() {
        return Strings.nullToEmpty((String) fields.get(GROUP_SEARCH_BASE));
    }

    @Override
    public void setGroupSearchBase(String groupSearchBase) {
        fields.put(GROUP_SEARCH_BASE, groupSearchBase);
    }

    @Override
    public String getGroupIdAttribute() {
        return Strings.nullToEmpty((String) fields.get(GROUP_ID_ATTRIBUTE));
    }

    @Override
    public void setGroupIdAttribute(String groupIdAttribute) {
        fields.put(GROUP_ID_ATTRIBUTE, groupIdAttribute);
    }

    @Override
    public String getGroupSearchPattern() {
        return Strings.nullToEmpty((String) fields.get(GROUP_SEARCH_PATTERN));
    }

    @Override
    public void setGroupSearchPattern(String groupSearchPattern) {
        fields.put(GROUP_SEARCH_PATTERN, groupSearchPattern);
    }

    @Override
    public Set<String> getAdditionalDefaultGroups() {
        final Set<String> additionalGroups = getAdditionalDefaultGroupIds();
        try {
            final Map<String, Role> idToRole = roleService.loadAllIdMap();
            return Collections2.transform(additionalGroups, Roles.roleIdToNameFunction(idToRole)).stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        } catch (NotFoundException e) {
            LOG.error("Unable to load role mapping");
            return Collections.emptySet();
        }
    }

    @Override
    public Set<String> getAdditionalDefaultGroupIds() {
        @SuppressWarnings("unchecked")
        final List<String> additionalGroups = (List<String>) fields.get(ADDITIONAL_DEFAULT_GROUPS);
        return additionalGroups == null ? Collections.<String>emptySet() : Sets.newHashSet(additionalGroups);
    }

    @Override
    public void setAdditionalDefaultGroups(Set<String> groupNames) {
        try {
            if (groupNames == null) return;

            final Map<String, Role> nameToRole = Maps.uniqueIndex(roleService.loadAll(), Roles.roleToNameFunction());
            final List<String> groupIds = Collections2.transform(groupNames, new Function<String, String>() {
                @Nullable
                @Override
                public String apply(@Nullable String groupName) {
                    if (groupName == null || !nameToRole.containsKey(groupName)) {
                        return null;
                    }
                    return nameToRole.get(groupName).getId();
                }
            }).stream().filter(Objects::nonNull).collect(Collectors.toList());
            fields.put(ADDITIONAL_DEFAULT_GROUPS, groupIds);
        } catch (NotFoundException e) {
            LOG.error("Unable to convert group names to ids", e);
            throw new IllegalStateException("Unable to convert group names to ids", e);
        }

    }
}
