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
package org.graylog.security.authservice.ldap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Ints;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.ExtendedResult;
import com.unboundid.ldap.sdk.FailoverServerSet;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPBindException;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import com.unboundid.ldap.sdk.extensions.StartTLSExtendedRequest;
import com.unboundid.util.Base64;
import com.unboundid.util.LDAPTestUtils;
import com.unboundid.util.ssl.SSLUtil;
import org.graylog2.security.TrustAllX509TrustManager;
import org.graylog2.security.TrustManagerProvider;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.net.SocketFactory;
import java.security.GeneralSecurityException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.unboundid.util.StaticUtils.isValidUTF8;
import static com.unboundid.util.StaticUtils.toUTF8String;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

// TODO: Possible improvements:
//   - Use a connection pool to improve performance and reduce load (see: https://docs.ldap.com/ldap-sdk/docs/getting-started/connection-pools.html)
//   - Support connecting to multiple servers for failover and load balancing (see: https://docs.ldap.com/ldap-sdk/docs/getting-started/failover-load-balancing.html)
@Singleton
public class UnboundLDAPConnector {
    private static final Logger LOG = LoggerFactory.getLogger(UnboundLDAPConnector.class);

    private static final String OBJECT_CLASS_ATTRIBUTE = "objectClass";

    private final int connectionTimeout;
    private final Set<String> enabledTlsProtocols;
    private final TrustManagerProvider trustManagerProvider;
    private final EncryptedValueService encryptedValueService;
    private final int requestTimeoutSeconds;

    @Inject
    public UnboundLDAPConnector(@Named("ldap_connection_timeout") int connectionTimeout,
                                @Named("enabled_tls_protocols") Set<String> enabledTlsProtocols,
                                TrustManagerProvider trustManagerProvider,
                                EncryptedValueService encryptedValueService) {
        this.connectionTimeout = connectionTimeout; // TODO: Make configurable per backend
        this.enabledTlsProtocols = enabledTlsProtocols;
        this.trustManagerProvider = trustManagerProvider;
        this.encryptedValueService = encryptedValueService;
        this.requestTimeoutSeconds = 60; // TODO: Make configurable per backend
    }

    public LDAPConnection connect(LDAPConnectorConfig ldapConfig) throws GeneralSecurityException, LDAPException {
        if (ldapConfig.serverList().isEmpty()) {
            LOG.warn("Cannot connect with empty server list");
            return null;
        }

        final String[] addresses = ldapConfig.serverList().stream().map(LDAPConnectorConfig.LDAPServer::hostname).toArray(String[]::new);
        final int[] ports = ldapConfig.serverList().stream().mapToInt(LDAPConnectorConfig.LDAPServer::port).toArray();

        final LDAPConnectionOptions connectionOptions = new LDAPConnectionOptions();
        connectionOptions.setUseReuseAddress(true);
        connectionOptions.setConnectTimeoutMillis(connectionTimeout);

        StartTLSExtendedRequest startTLSRequest = null;
        SocketFactory socketFactory = null;
        if (ldapConfig.transportSecurity() != LDAPTransportSecurity.NONE) {
            SSLUtil.setEnabledSSLProtocols(enabledTlsProtocols);

            final SSLUtil sslUtil;
            if (ldapConfig.verifyCertificates()) {
                sslUtil = new SSLUtil(trustManagerProvider.create(Arrays.asList(addresses)));
            } else {
                sslUtil = new SSLUtil(new TrustAllX509TrustManager());
            }

            if (ldapConfig.transportSecurity() == LDAPTransportSecurity.START_TLS) {
                // Use the StartTLS extended operation to secure the connection.
                startTLSRequest = new StartTLSExtendedRequest(sslUtil.createSSLContext());
            } else if (ldapConfig.transportSecurity() == LDAPTransportSecurity.TLS) {
                socketFactory = sslUtil.createSSLSocketFactory();
            }
        }

        final FailoverServerSet serverSet = new FailoverServerSet(addresses, ports, socketFactory, connectionOptions, null, null);

        final LDAPConnection connection = serverSet.getConnection();

        if (startTLSRequest != null) {
            final ExtendedResult startTLSResult = connection.processExtendedOperation(startTLSRequest);
            LDAPTestUtils.assertResultCodeEquals(startTLSResult, ResultCode.SUCCESS);
        }

        if (ldapConfig.systemUsername().isPresent()) {
            if (ldapConfig.systemPassword().isSet()) {
                final String systemPassword = encryptedValueService.decrypt(ldapConfig.systemPassword());
                final BindRequest bindRequest = new SimpleBindRequest(ldapConfig.systemUsername().get(), systemPassword);
                connection.bind(bindRequest);
            } else {
                LOG.warn("System username has been set to <{}> but no system password has been set. Skipping bind request.",
                        ldapConfig.systemUsername().get());
            }
        }
        return connection;
    }

    public ImmutableList<LDAPEntry> search(LDAPConnection connection,
                                           String searchBase,
                                           Filter filter,
                                           String uniqueIdAttribute,
                                           Set<String> attributes) throws LDAPException {
        final ImmutableSet<String> allAttributes = ImmutableSet.<String>builder()
                .add(OBJECT_CLASS_ATTRIBUTE)
                .addAll(attributes)
                .build();
        // TODO: Use LDAPEntrySource for a more memory efficient search
        final SearchRequest searchRequest = new SearchRequest(searchBase, SearchScope.SUB, filter, allAttributes.toArray(new String[0]));
        searchRequest.setTimeLimitSeconds(requestTimeoutSeconds);

        if (LOG.isTraceEnabled()) {
            LOG.trace("Search LDAP for <{}> using search base <{}>", filter.toNormalizedString(), searchBase);
        }

        final SearchResult searchResult = connection.search(searchRequest);
        if (searchResult.getSearchEntries().isEmpty()) {
            LOG.trace("No LDAP entry found for filter <{}>", filter.toNormalizedString());
            return ImmutableList.of();
        }

        return searchResult.getSearchEntries().stream()
                .map(entry -> createLDAPEntry(entry, uniqueIdAttribute))
                .collect(ImmutableList.toImmutableList());
    }

    public Optional<LDAPUser> searchUserByPrincipal(LDAPConnection connection,
                                                    UnboundLDAPConfig config,
                                                    String principal) throws LDAPException {
        final String filterString = new MessageFormat(config.userSearchPattern(), Locale.ENGLISH)
                .format(new Object[]{Filter.encodeValue(principal)});

        return searchUser(connection, config, Filter.create(filterString));
    }

    public Optional<LDAPUser> searchUserByUniqueId(LDAPConnection connection,
                                                   UnboundLDAPConfig config,
                                                   byte[] uniqueId) throws LDAPException {
        return searchUser(connection, config, Filter.createEqualityFilter(config.userUniqueIdAttribute(), uniqueId));
    }

    private Optional<LDAPUser> searchUser(LDAPConnection connection,
                                          UnboundLDAPConfig config,
                                          Filter filter) throws LDAPException {
        final ImmutableSet<String> allAttributes = ImmutableSet.<String>builder()
                .add("userPrincipalName") // TODO: This is ActiveDirectory specific - Do we need this here?
                .add("userAccountControl")
                .add("mail")
                .add("rfc822Mailbox")
                .add(config.userUniqueIdAttribute())
                .add(config.userNameAttribute())
                .add(config.userFullNameAttribute())
                .build();

        final ImmutableList<LDAPEntry> result = search(connection, config.userSearchBase(), filter, config.userUniqueIdAttribute(), allAttributes);

        if (result.size() > 1) {
            LOG.warn("Found more than one user for <{}> in search base <{}> - Using the first one", filter.toString(), config.userSearchBase());
        }

        return result.stream().findFirst()
                .map(entry -> createLDAPUser(config, entry));
    }

    public boolean authenticate(LDAPConnection connection, String bindDn, EncryptedValue password) throws LDAPException {
        checkArgument(!isNullOrEmpty(bindDn), "Binding with empty principal is forbidden.");
        checkArgument(password != null, "Binding with null credentials is forbidden.");
        checkArgument(password.isSet(), "Binding with empty credentials is forbidden.");

        final SimpleBindRequest bindRequest = new SimpleBindRequest(bindDn, encryptedValueService.decrypt(password));
        LOG.trace("Re-binding with DN <{}> using password", bindDn);

        try {
            final BindResult bind = connection.bind(bindRequest);

            if (!bind.getResultCode().equals(ResultCode.SUCCESS)) {
                LOG.trace("Re-binding DN <{}> failed", bindDn);
                throw new RuntimeException(bind.toString());
            }
            final boolean authenticated = connection.getLastBindRequest().equals(bindRequest);
            LOG.trace("Binding DN <{}> did not throw, connection authenticated: {}", bindDn, authenticated);
            return authenticated;
        } catch (LDAPBindException e) {
            LOG.trace("Re-binding DN <{}> failed", bindDn);
            return false;
        }
    }

    public LDAPEntry createLDAPEntry(Entry entry, String uniqueIdAttribute) {
        requireNonNull(entry, "entry cannot be null");
        checkArgument(!isBlank(uniqueIdAttribute), "uniqueIdAttribute cannot be blank");

        final LDAPEntry.Builder ldapEntryBuilder = LDAPEntry.builder();

        // Always set the proper DN for the entry
        ldapEntryBuilder.dn(entry.getDN());

        // Always require and set the unique ID attribute
        final byte[] uniqueId = requireNonNull(
                entry.getAttributeValueBytes(uniqueIdAttribute),
                uniqueIdAttribute + " attribute cannot be null"
        );
        ldapEntryBuilder.base64UniqueId(Base64.encode(uniqueId));

        if (entry.getObjectClassValues() != null) {
            ldapEntryBuilder.objectClasses(Arrays.asList(entry.getObjectClassValues()));
        }

        for (final Attribute attribute : entry.getAttributes()) {
            // No need to add the objectClass attribute to the attribute map, we already make it available
            // in LDAPEntry#objectClasses
            if (OBJECT_CLASS_ATTRIBUTE.equalsIgnoreCase(attribute.getBaseName())) {
                continue;
            }
            // We already set the unique ID above
            if (uniqueIdAttribute.equalsIgnoreCase(attribute.getBaseName())) {
                continue;
            }

            if (attribute.needsBase64Encoding()) {
                for (final byte[] value : attribute.getValueByteArrays()) {
                    if (isValidUTF8(value)) {
                        ldapEntryBuilder.addAttribute(attribute.getBaseName(), toUTF8String(value));
                    } else {
                        ldapEntryBuilder.addAttribute(attribute.getBaseName(), Base64.encode(value));
                    }
                }
            } else {
                for (final String value : attribute.getValues()) {
                    ldapEntryBuilder.addAttribute(attribute.getBaseName(), value);
                }
            }
        }

        return ldapEntryBuilder.build();
    }

    public LDAPUser createLDAPUser(UnboundLDAPConfig config, Entry entry) {
        return createLDAPUser(config, createLDAPEntry(entry, config.userUniqueIdAttribute()));
    }

    public LDAPUser createLDAPUser(UnboundLDAPConfig config, LDAPEntry ldapEntry) {
        final String username = ldapEntry.nonBlankAttribute(config.userNameAttribute());
        return LDAPUser.builder()
                .base64UniqueId(ldapEntry.base64UniqueId())
                .accountIsEnabled(findAccountIsEnabled(ldapEntry))
                .username(username)
                .fullName(ldapEntry.firstAttributeValue(config.userFullNameAttribute()).orElse(username))
                .email(ldapEntry.firstAttributeValue("mail").orElse(ldapEntry.firstAttributeValue("rfc822Mailbox").orElse("unknown@unknown.invalid")))
                .entry(ldapEntry)
                .build();
    }

    private boolean findAccountIsEnabled(LDAPEntry ldapEntry) {
        final Optional<String> control = ldapEntry.firstAttributeValue("userAccountControl");

        // No field present. Assume account is enabled
        if (!control.isPresent()) {
            return true;
        }
        final Integer userAccountControl = Ints.tryParse(control.get());
        if (userAccountControl == null) {
            LOG.warn("Ignoring non-parseable userAccountControl value");
            return true;
        }
        return !ADUserAccountControl.create(userAccountControl).accountIsDisabled();
    }
}
