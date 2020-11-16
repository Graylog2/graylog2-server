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
package org.graylog2.migrations;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Sorts;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog.security.authservice.AuthServiceBackendDTO;
import org.graylog.security.authservice.DBAuthServiceBackendService;
import org.graylog.security.authservice.backend.ADAuthServiceBackendConfig;
import org.graylog.security.authservice.backend.LDAPAuthServiceBackendConfig;
import org.graylog.security.authservice.ldap.LDAPTransportSecurity;
import org.graylog.security.authzroles.AuthzRoleDTO;
import org.graylog.security.authzroles.PaginatedAuthzRolesService;
import org.graylog2.Configuration;
import org.graylog2.database.MongoConnection;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.security.AESTools;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class V20201103145400_LegacyAuthServiceMigration extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20201103145400_LegacyAuthServiceMigration.class);

    public interface MigrationModule {
        void upgrade(Document document, AuthServiceBackendDTO authServiceConfig);
    }

    private final MongoCollection<Document> ldapSettings;
    private final Set<MigrationModule> migrationModules;
    private final EncryptedValueService encryptedValueService;
    private final PaginatedAuthzRolesService rolesService;
    private final DBAuthServiceBackendService authServiceBackendService;
    private final NotificationService notificationService;
    private final ClusterConfigService clusterConfigService;
    private final String encryptionKey;

    @Inject
    public V20201103145400_LegacyAuthServiceMigration(MongoConnection mongoConnection,
                                                      Set<MigrationModule> migrationModules,
                                                      EncryptedValueService encryptedValueService,
                                                      Configuration configuration,
                                                      PaginatedAuthzRolesService rolesService,
                                                      DBAuthServiceBackendService authServiceBackendService,
                                                      NotificationService notificationService,
                                                      ClusterConfigService clusterConfigService) {
        this.ldapSettings = mongoConnection.getMongoDatabase().getCollection("ldap_settings");
        this.migrationModules = migrationModules;
        this.encryptedValueService = encryptedValueService;
        this.encryptionKey = configuration.getPasswordSecret().substring(0, 16);
        this.rolesService = rolesService;
        this.authServiceBackendService = authServiceBackendService;
        this.notificationService = notificationService;
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2020-11-03T14:54:00Z");
    }

    @Override
    public void upgrade() {
        final MigrationCompleted migrationState = clusterConfigService.getOrDefault(MigrationCompleted.class, MigrationCompleted.createEmpty());
        final ImmutableSet.Builder<String> migratedConfigsBuilder = ImmutableSet.builder();

        // While the LDAP settings collection could contain more than one document, in practice we only expect a
        // single one. That's why we are using the ID of the last created auth service for the notification.
        String lastCreatedAuthServiceId = null;

        // Add all configs that have already been migrated
        migratedConfigsBuilder.addAll(migrationState.migratedConfigs());

        for (final Document document : ldapSettings.find().sort(Sorts.ascending("_id"))) {
            final String idString = document.getObjectId("_id").toHexString();

            if (!document.getBoolean("enabled")) {
                LOG.debug("Skipping disabled configuration <{}>", idString);
                continue;
            }

            if (migrationState.isDone(idString)) {
                LOG.debug("Configuration <{}> already migrated", idString);
                continue;
            }

            final AuthServiceBackendDTO newConfig;
            if (document.getBoolean("active_directory")) {
                newConfig = buildActiveDirectoryConfig(document);
            } else {
                newConfig = buildLDAPConfig(document);
            }

            final AuthServiceBackendDTO savedConfig = authServiceBackendService.save(newConfig);

            for (final MigrationModule migrationModule : migrationModules) {
                migrationModule.upgrade(document, savedConfig);
            }

            lastCreatedAuthServiceId = savedConfig.id();

            migratedConfigsBuilder.add(idString);
        }

        final ImmutableSet<String> migratedConfigs = migratedConfigsBuilder.build();
        clusterConfigService.write(MigrationCompleted.create(migratedConfigs));

        if (lastCreatedAuthServiceId != null) {
            final Notification notification = notificationService.buildNow()
                    .addType(Notification.Type.LEGACY_LDAP_CONFIG_MIGRATION)
                    .addSeverity(Notification.Severity.URGENT)
                    .addDetail("auth_service_id", lastCreatedAuthServiceId);
            notificationService.publishIfFirst(notification);
        }
    }

    private AuthServiceBackendDTO buildActiveDirectoryConfig(Document document) {
        return AuthServiceBackendDTO.builder()
                .title(getTitle(document, "Active Directory"))
                .description("Migrated from legacy Active Directory configuration.")
                .defaultRoles(getDefaultRoles(document))
                .config(ADAuthServiceBackendConfig.builder()
                        .servers(Collections.singletonList(getADHostAndPort(document)))
                        .transportSecurity(getTransportSecurity(document))
                        .verifyCertificates(getVerifyCertificates(document))
                        .systemUserDn(document.getString("system_username"))
                        .systemUserPassword(getSystemUserPassword(document))
                        .userSearchBase(document.getString("search_base"))
                        .userSearchPattern(document.getString("principal_search_pattern"))
                        .userNameAttribute("sAMAccountName")
                        .userFullNameAttribute(document.getString("username_attribute"))
                        .build())
                .build();
    }

    private AuthServiceBackendDTO buildLDAPConfig(Document document) {
        return AuthServiceBackendDTO.builder()
                .title(getTitle(document, "LDAP"))
                .description("Migrated from legacy LDAP configuration.")
                .defaultRoles(getDefaultRoles(document))
                .config(LDAPAuthServiceBackendConfig.builder()
                        .servers(Collections.singletonList(getLDAPHostAndPort(document)))
                        .transportSecurity(getTransportSecurity(document))
                        .verifyCertificates(getVerifyCertificates(document))
                        .systemUserDn(document.getString("system_username"))
                        .systemUserPassword(getSystemUserPassword(document))
                        .userSearchBase(document.getString("search_base"))
                        .userSearchPattern(document.getString("principal_search_pattern"))
                        .userUniqueIdAttribute("entryUUID")
                        .userNameAttribute("uid")
                        .userFullNameAttribute(document.getString("username_attribute"))
                        .build())
                .build();
    }

    private String getTitle(Document document, String prefix) {
        final String ldapUriString = document.getString("ldap_uri");
        return String.format(Locale.US, "%s- %s", prefix, ldapUriString);
    }

    private LDAPAuthServiceBackendConfig.HostAndPort getLDAPHostAndPort(Document document) {
        final String ldapUriString = document.getString("ldap_uri");
        final URI ldapUri = URI.create(ldapUriString);

        return LDAPAuthServiceBackendConfig.HostAndPort.create(ldapUri.getHost(), ldapUri.getPort());
    }

    private ADAuthServiceBackendConfig.HostAndPort getADHostAndPort(Document document) {
        final String ldapUriString = document.getString("ldap_uri");
        final URI ldapUri = URI.create(ldapUriString);

        return ADAuthServiceBackendConfig.HostAndPort.create(ldapUri.getHost(), ldapUri.getPort());
    }

    private LDAPTransportSecurity getTransportSecurity(Document document) {
        final String ldapUriString = document.getString("ldap_uri");
        final Boolean isStartTLS = document.getBoolean("use_start_tls");

        if (isStartTLS) {
            return LDAPTransportSecurity.START_TLS;
        } else if (ldapUriString.toLowerCase(Locale.US).startsWith("ldaps://")) {
            return LDAPTransportSecurity.TLS;
        } else {
            return LDAPTransportSecurity.NONE;
        }
    }

    private boolean getVerifyCertificates(Document document) {
        return !document.getBoolean("trust_all_certificates", false);
    }

    private EncryptedValue getSystemUserPassword(Document document) {
        final String encryptedPassword = document.getString("system_password");
        final String salt = document.getString("system_password_salt");

        if (isNotBlank(encryptedPassword) && isNotBlank(salt)) {
            return encryptedValueService.encrypt(AESTools.decrypt(encryptedPassword, encryptionKey, salt));
        }

        return EncryptedValue.createUnset();
    }

    private Set<String> getDefaultRoles(Document document) {
        final String defaultRole = document.getString("default_group");
        final List<String> additionalDefaultRoles = document.getList("additional_default_groups", String.class, Collections.emptyList());

        final Set<String> roleIds = Stream.concat(Stream.of(defaultRole), additionalDefaultRoles.stream())
                .filter(StringUtils::isNotBlank)
                .filter(ObjectId::isValid)
                .collect(Collectors.toSet());

        // Only return roles that actually exist
        return rolesService.findByIds(roleIds).stream()
                .map(AuthzRoleDTO::id)
                .collect(Collectors.toSet());
    }

    @AutoValue
    public static abstract class MigrationCompleted {
        @JsonProperty("migrated_configs")
        public abstract Set<String> migratedConfigs();

        public boolean isDone(String id) {
            return migratedConfigs().contains(id);
        }

        @JsonCreator
        public static MigrationCompleted create(@JsonProperty("migrated_configs") Set<String> migratedConfigs) {
            return new AutoValue_V20201103145400_LegacyAuthServiceMigration_MigrationCompleted(migratedConfigs);
        }

        public static MigrationCompleted createEmpty() {
            return create(Collections.emptySet());
        }
    }
}
