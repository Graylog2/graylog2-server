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
package org.graylog.security.authservice.ldap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
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
import static java.util.Objects.requireNonNull;

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
                // TODO support multiple hosts
                sslUtil = new SSLUtil(trustManagerProvider.create(addresses[0]));
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
            final String systemPassword = encryptedValueService.decrypt(ldapConfig.systemPassword());
            final BindRequest bindRequest = new SimpleBindRequest(ldapConfig.systemUsername().get(), systemPassword);
            connection.bind(bindRequest);
        }
        return connection;
    }

    public ImmutableList<LDAPEntry> search(LDAPConnection connection,
                                           String searchBase,
                                           Filter filter,
                                           Set<String> attributes) throws LDAPException {
        final ImmutableSet<String> allAttributes = ImmutableSet.<String>builder()
                .add(OBJECT_CLASS_ATTRIBUTE)
                .addAll(attributes)
                .build();
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
                .map(this::createLDAPEntry)
                .collect(ImmutableList.toImmutableList());
    }

    public Optional<LDAPUser> searchUser(LDAPConnection connection,
                                         UnboundLDAPConfig config,
                                         String principal) throws LDAPException {
        final String filterString = new MessageFormat(config.userSearchPattern(), Locale.ENGLISH).format(new Object[]{Filter.encodeValue(principal)});
        final Filter filter = Filter.create(filterString);

        final ImmutableSet<String> allAttributes = ImmutableSet.<String>builder()
                .add("userPrincipalName") // TODO: This is ActiveDirectory specific - Do we need this here?
                .add("mail")
                .add("rfc822Mailbox")
                .add(config.userUniqueIdAttribute())
                .add(config.userNameAttribute())
                .add(config.userFullNameAttribute())
                .build();

        final ImmutableList<LDAPEntry> result = search(connection, config.userSearchBase(), filter, allAttributes);

        if (result.size() > 1) {
            LOG.warn("Found more than one user for <{}> in search base <{}> - Using the first one", filterString, config.userSearchBase());
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

    public LDAPEntry createLDAPEntry(Entry entry) {
        requireNonNull(entry, "entry cannot be null");

        final LDAPEntry.Builder ldapEntryBuilder = LDAPEntry.builder();

        // Always set the proper DN for the entry
        ldapEntryBuilder.dn(entry.getDN());

        if (entry.getObjectClassValues() != null) {
            ldapEntryBuilder.objectClasses(Arrays.asList(entry.getObjectClassValues()));
        }

        for (final Attribute attribute : entry.getAttributes()) {
            // No need to add the objectClass attribute to the attribute map, we already make it available
            // in LDAPEntry#objectClasses
            if (OBJECT_CLASS_ATTRIBUTE.equalsIgnoreCase(attribute.getBaseName())) {
                continue;
            }

            if (attribute.needsBase64Encoding()) {
                for (final byte[] value : attribute.getValueByteArrays()) {
                    ldapEntryBuilder.addAttribute(attribute.getBaseName(), Base64.encode(value));
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
        return createLDAPUser(config, createLDAPEntry(entry));
    }

    public LDAPUser createLDAPUser(UnboundLDAPConfig config, LDAPEntry ldapEntry) {
        return LDAPUser.builder()
                .uniqueId(ldapEntry.nonBlankAttribute(config.userUniqueIdAttribute()))
                .username(ldapEntry.nonBlankAttribute(config.userNameAttribute()))
                .fullName(ldapEntry.nonBlankAttribute(config.userFullNameAttribute()))
                .email(ldapEntry.firstAttributeValue("mail").orElse(ldapEntry.firstAttributeValue("rfc822Mailbox").orElse("")))
                .entry(ldapEntry)
                .build();
    }
}
