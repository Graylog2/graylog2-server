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
package org.graylog.security.authservice.backend;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import org.apache.commons.lang3.StringUtils;
import org.graylog.security.authservice.AuthServiceBackendConfig;
import org.graylog.security.authservice.ldap.LDAPConnectorConfig;
import org.graylog.security.authservice.ldap.LDAPConnectorConfigProvider;
import org.graylog.security.authservice.ldap.LDAPTransportSecurity;
import org.graylog2.plugin.rest.ValidationResult;
import org.graylog2.security.encryption.EncryptedValue;

import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;

@AutoValue
@JsonDeserialize(builder = ADAuthServiceBackendConfig.Builder.class)
@JsonTypeName(ADAuthServiceBackend.TYPE_NAME)
public abstract class ADAuthServiceBackendConfig implements AuthServiceBackendConfig, LDAPConnectorConfigProvider {
    private static final String FIELD_SERVERS = "servers";
    private static final String FIELD_TRANSPORT_SECURITY = "transport_security";
    private static final String FIELD_VERIFY_CERTIFICATES = "verify_certificates";
    private static final String FIELD_SYSTEM_USER_DN = "system_user_dn";
    private static final String FIELD_SYSTEM_USER_PASSWORD = "system_user_password";
    private static final String FIELD_USER_SEARCH_BASE = "user_search_base";
    private static final String FIELD_USER_SEARCH_PATTERN = "user_search_pattern";
    private static final String FIELD_USER_NAME_ATTRIBUTE = "user_name_attribute";
    private static final String FIELD_USER_FULL_NAME_ATTRIBUTE = "user_full_name_attribute";

    @JsonProperty(FIELD_SERVERS)
    public abstract ImmutableList<HostAndPort> servers();

    @JsonProperty(FIELD_TRANSPORT_SECURITY)
    public abstract LDAPTransportSecurity transportSecurity();

    @JsonProperty(FIELD_VERIFY_CERTIFICATES)
    public abstract boolean verifyCertificates();

    @JsonProperty(FIELD_SYSTEM_USER_DN)
    public abstract String systemUserDn();

    @JsonProperty(FIELD_SYSTEM_USER_PASSWORD)
    public abstract EncryptedValue systemUserPassword();

    @JsonProperty(FIELD_USER_SEARCH_BASE)
    public abstract String userSearchBase();

    @JsonProperty(FIELD_USER_SEARCH_PATTERN)
    public abstract String userSearchPattern();

    @JsonProperty(FIELD_USER_NAME_ATTRIBUTE)
    public abstract String userNameAttribute();

    @JsonProperty(FIELD_USER_FULL_NAME_ATTRIBUTE)
    public abstract String userFullNameAttribute();

    @Override
    public void validate(ValidationResult result) {
        if (servers().size() > 1) {
            result.addError(FIELD_SERVERS, "Currently only a single server URL is supported.");
        }
        if (isBlank(userSearchBase())) {
            result.addError(FIELD_USER_SEARCH_BASE, "User search base cannot be empty.");
        }
        if (isBlank(userSearchPattern())) {
            result.addError(FIELD_USER_SEARCH_PATTERN, "User search pattern cannot be empty.");
        } else {
            try {
                Filter.create(userSearchPattern());
            } catch (LDAPException e) {
                result.addError(FIELD_USER_SEARCH_PATTERN, "User search pattern cannot be parsed. It must be a valid LDAP filter.");
            }
        }
        if (isBlank(userNameAttribute())) {
            result.addError(FIELD_USER_NAME_ATTRIBUTE, "User name attribute cannot be empty.");
        }
        if (isBlank(userFullNameAttribute())) {
            result.addError(FIELD_USER_FULL_NAME_ATTRIBUTE, "User full name cannot be empty.");
        }
    }

    @Override
    public LDAPConnectorConfig getLDAPConnectorConfig() {
        return LDAPConnectorConfig.builder()
                .serverList(servers().stream()
                        .map(hap -> LDAPConnectorConfig.LDAPServer.create(hap.host(), hap.port()))
                        .collect(Collectors.toList()))
                .systemUsername(StringUtils.trimToNull(systemUserDn()))
                .systemPassword(systemUserPassword())
                .transportSecurity(transportSecurity())
                .verifyCertificates(verifyCertificates())
                .build();
    }

    public abstract Builder toBuilder();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder implements AuthServiceBackendConfig.Builder<Builder> {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_ADAuthServiceBackendConfig.Builder()
                    .type(ADAuthServiceBackend.TYPE_NAME)
                    .verifyCertificates(true)
                    .systemUserDn("")
                    .systemUserPassword(EncryptedValue.createUnset())
                    .userSearchPattern(ADAuthServiceBackend.AD_DEFAULT_USER_SEARCH_PATTERN.toNormalizedString())
                    .userNameAttribute(ADAuthServiceBackend.AD_USER_PRINCIPAL_NAME)
                    .userFullNameAttribute(ADAuthServiceBackend.AD_DISPLAY_NAME);
        }

        @JsonProperty(FIELD_SERVERS)
        public abstract Builder servers(List<HostAndPort> servers);

        @JsonProperty(FIELD_TRANSPORT_SECURITY)
        public abstract Builder transportSecurity(LDAPTransportSecurity transportSecurity);

        @JsonProperty(FIELD_VERIFY_CERTIFICATES)
        public abstract Builder verifyCertificates(boolean verifyCertificates);

        @JsonProperty(FIELD_SYSTEM_USER_DN)
        public abstract Builder systemUserDn(String systemUserDn);

        @JsonProperty(FIELD_SYSTEM_USER_PASSWORD)
        public abstract Builder systemUserPassword(EncryptedValue systemUserPassword);

        @JsonProperty(FIELD_USER_SEARCH_BASE)
        public abstract Builder userSearchBase(String userSearchBase);

        @JsonProperty(FIELD_USER_SEARCH_PATTERN)
        public abstract Builder userSearchPattern(String userSearchPattern);

        @JsonProperty(FIELD_USER_NAME_ATTRIBUTE)
        public abstract Builder userNameAttribute(String userNameAttribute);

        @JsonProperty(FIELD_USER_FULL_NAME_ATTRIBUTE)
        public abstract Builder userFullNameAttribute(String userFullNameAttribute);

        public abstract ADAuthServiceBackendConfig build();
    }

    @AutoValue
    public static abstract class HostAndPort {
        @JsonProperty("host")
        public abstract String host();

        @JsonProperty("port")
        public abstract int port();

        @JsonCreator
        public static HostAndPort create(@JsonProperty("host") String host, @JsonProperty("port") int port) {
            return new AutoValue_ADAuthServiceBackendConfig_HostAndPort(host, port);
        }

        @Override
        public String toString() {
            return host() + ":" + port();
        }
    }
}
