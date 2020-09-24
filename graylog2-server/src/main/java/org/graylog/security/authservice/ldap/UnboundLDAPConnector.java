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

import com.google.common.base.Joiner;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.BindResult;
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
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import com.unboundid.ldap.sdk.extensions.StartTLSExtendedRequest;
import com.unboundid.util.LDAPTestUtils;
import com.unboundid.util.ssl.SSLUtil;
import org.graylog2.security.TrustAllX509TrustManager;
import org.graylog2.security.TrustManagerProvider;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.security.ldap.LdapEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.net.SocketFactory;
import java.security.GeneralSecurityException;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

// TODO: Possible improvements:
//   - Use a connection pool to improve performance and reduce load (see: https://docs.ldap.com/ldap-sdk/docs/getting-started/connection-pools.html)
//   - Support connecting to multiple servers for failover and load balancing (see: https://docs.ldap.com/ldap-sdk/docs/getting-started/failover-load-balancing.html)
@Singleton
public class UnboundLDAPConnector {
    private static final Logger LOG = LoggerFactory.getLogger(UnboundLDAPConnector.class);

    private final int connectionTimeout;
    private final Set<String> enabledTlsProtocols;
    private final TrustManagerProvider trustManagerProvider;
    private final EncryptedValueService encryptedValueService;

    @Inject
    public UnboundLDAPConnector(@Named("ldap_connection_timeout") int connectionTimeout,
                                @Named("enabled_tls_protocols") Set<String> enabledTlsProtocols,
                                TrustManagerProvider trustManagerProvider,
                                EncryptedValueService encryptedValueService) {
        this.connectionTimeout = connectionTimeout;
        this.enabledTlsProtocols = enabledTlsProtocols;
        this.trustManagerProvider = trustManagerProvider;
        this.encryptedValueService = encryptedValueService;
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

    @Nullable
    public LdapEntry search(LDAPConnection connection,
                            String searchBase,
                            String userSearchPattern,
                            String displayNameAttribute,
                            String uniqueIdAttribute,
                            String principal) throws LDAPException {
        final LdapEntry ldapEntry = new LdapEntry();

        final String filterString = new MessageFormat(userSearchPattern, Locale.ENGLISH).format(new Object[]{Filter.encodeValue(principal)});
        final Filter filter = Filter.create(filterString);
        final SearchRequest searchRequest = new SearchRequest(
                searchBase,
                SearchScope.SUB,
                filter,
                displayNameAttribute, uniqueIdAttribute, "dn", "uid", "userPrincipalName", "mail", "rfc822Mailbox"
        );

        if (LOG.isTraceEnabled()) {
            LOG.trace("Search LDAP for {}, starting at {}", filterString, searchBase);
        }

        final SearchResult searchResult = connection.search(searchRequest);
        if (searchResult.getSearchEntries().isEmpty()) {
            LOG.trace("No LDAP entry found for filter {}", filterString);
            return null;
        }
        for (SearchResultEntry entry : searchResult.getSearchEntries()) {
            // Always set the proper DN for the entry
            ldapEntry.setDn(entry.getDN());

            // for generic LDAP use the dn of the entry for the subsequent bind, active directory needs the userPrincipalName attribute (set below)
            ldapEntry.setBindPrincipal(entry.getDN());

            for (Attribute attribute : entry.getAttributes()) {
                if (!attribute.needsBase64Encoding()) {
                    ldapEntry.put(attribute.getBaseName(), Joiner.on(", ").join(attribute.getValues()));
                }
            }
        }
        return ldapEntry;
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
}
