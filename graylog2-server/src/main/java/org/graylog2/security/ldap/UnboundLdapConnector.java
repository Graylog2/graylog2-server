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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.ExtendedResult;
import com.unboundid.ldap.sdk.FailoverServerSet;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import com.unboundid.ldap.sdk.extensions.StartTLSExtendedRequest;
import com.unboundid.util.LDAPTestUtils;
import com.unboundid.util.ssl.SSLUtil;
import org.graylog.security.authservices.LdapConfig;
import org.graylog2.rest.models.system.ldap.requests.LdapTestConfigRequest;
import org.graylog2.security.AESToolsService;
import org.graylog2.security.TrustAllX509TrustManager;
import org.graylog2.shared.security.ldap.LdapEntry;
import org.graylog2.shared.security.ldap.LdapSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

public class UnboundLdapConnector {
    private static final Logger LOG = LoggerFactory.getLogger(UnboundLdapConnector.class);

    private static final String ATTRIBUTE_UNIQUE_MEMBER = "uniqueMember";
    private static final String ATTRIBUTE_MEMBER = "member";
    private static final String ATTRIBUTE_MEMBER_UID = "memberUid";

    private final int connectionTimeout;
    private final Set<String> enabledTlsProtocols;
    private final LdapSettingsService ldapSettingsService;
    private final TrustManagerProvider trustManagerProvider;
    private final AESToolsService aesToolsService;

    public interface TrustManagerProvider {
        TrustManager create(String host) throws KeyStoreException, NoSuchAlgorithmException;
    }

    @Inject
    public UnboundLdapConnector(@Named("ldap_connection_timeout") int connectionTimeout,
                                @Named("enabled_tls_protocols") Set<String> enabledTlsProtocols,
                                LdapSettingsService ldapSettingsService,
                                TrustManagerProvider trustManagerProvider,
                                AESToolsService aesToolsService) {
        this.connectionTimeout = connectionTimeout;
        this.enabledTlsProtocols = enabledTlsProtocols;
        this.ldapSettingsService = ldapSettingsService;
        this.trustManagerProvider = trustManagerProvider;
        this.aesToolsService = aesToolsService;
    }

    public LDAPConnection connect(LdapSettings settings) throws GeneralSecurityException, LDAPException {
        final LdapConfig ldapConfig = createConfig(settings);
        return connect(ldapConfig);
    }

    public LDAPConnection connect(LdapTestConfigRequest request) throws GeneralSecurityException, LDAPException {
        final LdapConfig ldapConfig = createConfig(request);
        return connect(ldapConfig);
    }

    public LDAPConnection connect(LdapConfig ldapConfig) throws GeneralSecurityException, LDAPException {
        if (ldapConfig.serverList().isEmpty()) {
            LOG.warn("Cannot connect with empty server list");
            return null;
        }

        String[] addresses = ldapConfig.serverList().stream().map(LdapConfig.LdapServer::hostname).toArray(String[]::new);
        int[] ports = ldapConfig.serverList().stream().mapToInt(LdapConfig.LdapServer::port).toArray();

        final LDAPConnectionOptions connectionOptions = new LDAPConnectionOptions();
        connectionOptions.setUseReuseAddress(true);
        connectionOptions.setConnectTimeoutMillis(connectionTimeout);

        StartTLSExtendedRequest startTLSRequest = null;
        SocketFactory socketFactory = null;
        if (ldapConfig.encyrption() != LdapConfig.EncryptionSetting.NONE) {
            SSLUtil.setEnabledSSLProtocols(enabledTlsProtocols);
            SSLUtil sslUtil;
            if (ldapConfig.verifyCertificates()) {
                // TODO support multiple hosts
                sslUtil = new SSLUtil(trustManagerProvider.create(addresses[0]));
            } else {
                sslUtil = new SSLUtil(new TrustAllX509TrustManager());
            }

            if (ldapConfig.encyrption() == LdapConfig.EncryptionSetting.START_TLS) {
                SSLContext sslContext = sslUtil.createSSLContext();
                // Use the StartTLS extended operation to secure the connection.
                startTLSRequest = new StartTLSExtendedRequest(sslContext);
            } else if (ldapConfig.encyrption() == LdapConfig.EncryptionSetting.SSL) {
                socketFactory = sslUtil.createSSLSocketFactory();
            }
        }

        final FailoverServerSet serverSet = new FailoverServerSet(addresses, ports, socketFactory, connectionOptions, null, null);

        final LDAPConnection connection = serverSet.getConnection();

        if (startTLSRequest != null) {
            ExtendedResult startTLSResult = connection.processExtendedOperation(startTLSRequest);
            LDAPTestUtils.assertResultCodeEquals(startTLSResult, ResultCode.SUCCESS);
        }

        if (ldapConfig.systemUsername().isPresent()) {
            String systemPassword = aesToolsService.decrypt(ldapConfig.encryptedSystemPassword());
            final BindRequest bindRequest = new SimpleBindRequest(ldapConfig.systemUsername().get(), systemPassword);
            connection.bind(bindRequest);
        }
        return connection;
    }

    @Nullable
    public LdapEntry search(LDAPConnection connection,
                            String searchBase,
                            String searchPattern,
                            String displayNameAttribute,
                            String principal,
                            boolean activeDirectory,
                            String groupSearchBase,
                            String groupIdAttribute,
                            String groupSearchPattern) throws LDAPException {
        final LdapEntry ldapEntry = new LdapEntry();

        //TODO check if this can be done with a Filter object
        final String filterString = new MessageFormat(searchPattern, Locale.ENGLISH).format(new Object[]{sanitizePrincipal(principal)});
        final Filter filter = Filter.create(filterString);
        SearchRequest searchRequest = new SearchRequest(
                searchBase,
                com.unboundid.ldap.sdk.SearchScope.SUB,
                filter,
                groupIdAttribute, displayNameAttribute, "dn", "uid", "userPrincipalName", "mail", "rfc822Mailbox", "memberOf", "isMemberOf");

        if (LOG.isTraceEnabled()) {
            LOG.trace("Search {} for {}, starting at {}",
                      activeDirectory ? "ActiveDirectory" : "LDAP", filterString, searchBase);
        }

        final SearchResult searchResult = connection.search(searchRequest);
        if (searchResult.getSearchEntries().isEmpty()) {
            LOG.trace("No LDAP entry found for filter {}", filterString);
            return null;
        }
        for (SearchResultEntry entry : searchResult.getSearchEntries()) {
            // always set the proper DN for the entry, we need it for group matching
            ldapEntry.setDn(entry.getDN());

            // for generic LDAP use the dn of the entry for the subsequent bind, active directory needs the userPrincipalName attribute (set below)
            ldapEntry.setBindPrincipal(entry.getDN());

            for (com.unboundid.ldap.sdk.Attribute attribute : entry.getAttributes()) {
                if (!attribute.needsBase64Encoding()) {
                    ldapEntry.put(attribute.getBaseName(), Joiner.on(", ").join(attribute.getValues()));
                }
            }
            // TODO group code goes here
        }
        return ldapEntry;
    }

    /**
     * Escapes any special chars (RFC 4515) from a string representing a
     * a search filter assertion value.
     *
     * @param input The input string.
     * @return A assertion value string ready for insertion into a
     * search filter string.
     */
    private String sanitizePrincipal(final String input) {
        String s = "";

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == '*') {
                // escape asterisk
                s += "\\2a";
            } else if (c == '(') {
                // escape left parenthesis
                s += "\\28";
            } else if (c == ')') {
                // escape right parenthesis
                s += "\\29";
            } else if (c == '\\') {
                // escape backslash
                s += "\\5c";
            } else if (c == '\u0000') {
                // escape NULL char
                s += "\\00";
            } else if (c <= 0x7f) {
                // regular 1-byte UTF-8 char
                s += String.valueOf(c);
            } else if (c >= 0x080) {
                // higher-order 2, 3 and 4-byte UTF-8 chars
                byte[] utf8bytes = String.valueOf(c).getBytes(StandardCharsets.UTF_8);

                for (byte b : utf8bytes) {
                    s += String.format(Locale.ENGLISH, "\\%02x", b);
                }
            }
        }

        return s;
    }

    public boolean authenticate(LDAPConnection connection, String principal, String credentials) throws LDAPException {
        checkArgument(!isNullOrEmpty(principal), "Binding with empty principal is forbidden.");
        checkArgument(!isNullOrEmpty(credentials), "Binding with empty credentials is forbidden.");

        SimpleBindRequest bindRequest = new SimpleBindRequest(principal, credentials);
        LOG.trace("Re-binding with DN {} using password", principal);

        final BindResult bind = connection.bind(bindRequest);

        if (!bind.getResultCode().equals(ResultCode.SUCCESS)) {
            LOG.trace("Re-binding DN {} failed", principal);
            throw new RuntimeException(bind.toString());
        }
        final boolean authenticated = connection.getLastBindRequest().equals(bindRequest);
        LOG.trace("Binding DN {} did not throw, connection authenticated: {}", principal, authenticated);
        return authenticated;
    }

    private LdapConfig createConfig(LdapSettings settings) {
        final LdapConfig.Builder config = LdapConfig.builder();
        final URI ldapUri = settings.getUri();
        config.serverList(ImmutableList.of(LdapConfig.LdapServer.create(ldapUri.getHost(), ldapUri.getPort())));

        if (ldapUri.getScheme().startsWith("ldaps")) {
            config.encyrption(LdapConfig.EncryptionSetting.SSL);
        } else if (settings.isUseStartTls()) {
            config.encyrption(LdapConfig.EncryptionSetting.START_TLS);
        } else {
            config.encyrption(LdapConfig.EncryptionSetting.NONE);
        }
        config.verifyCertificates(!settings.isTrustAllCertificates());

        if (!isNullOrEmpty(settings.getSystemUserName())) {
            config.systemUsername(settings.getSystemUserName());

            if (!isNullOrEmpty(settings.getSystemPassword())) {
                // Use the given password for the connection test
                config.encryptedSystemPassword(aesToolsService.encrypt(settings.getSystemPassword()));
            }
        }
        return config.build();
    }

    private LdapConfig createConfig(LdapTestConfigRequest request) {
        final LdapConfig.Builder config = LdapConfig.builder();
        final URI ldapUri = request.ldapUri();
        config.serverList(ImmutableList.of(LdapConfig.LdapServer.create(ldapUri.getHost(), ldapUri.getPort())));
        if (ldapUri.getScheme().startsWith("ldaps")) {
            config.encyrption(LdapConfig.EncryptionSetting.SSL);
        } else if (request.useStartTls()) {
            config.encyrption(LdapConfig.EncryptionSetting.START_TLS);
        } else {
            config.encyrption(LdapConfig.EncryptionSetting.NONE);
        }
        config.verifyCertificates(!request.trustAllCertificates());

        if (!isNullOrEmpty(request.systemUsername())) {
            config.systemUsername(request.systemUsername());

            if (!isNullOrEmpty(request.systemPassword())) {
                // Use the given password for the connection test
                config.encryptedSystemPassword(aesToolsService.encrypt(request.systemPassword()));
            } else {
                // If the config request has a username but no password set, we have to use the password from the database.
                // This is because we don't expose the plain-text password through the API anymore and the settings form
                // doesn't submit a password.
                final LdapSettings ldapSettings = ldapSettingsService.load();
                if (ldapSettings != null && !isNullOrEmpty(ldapSettings.getSystemPassword())) {
                    config.encryptedSystemPassword(aesToolsService.encrypt(ldapSettings.getSystemPassword()));
                }
            }
        }
        return config.build();
    }
}
