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

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.BindRequestImpl;
import org.apache.directory.api.ldap.model.message.BindResponse;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class LdapConnector {
    private static final Logger LOG = LoggerFactory.getLogger(LdapConnector.class);

    private final int connectionTimeout;

    public LdapConnector(final int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public LdapNetworkConnection connect(LdapConnectionConfig config) throws LdapException {
        final LdapNetworkConnection connection = new LdapNetworkConnection(config);
        connection.setTimeOut(connectionTimeout);

        if (LOG.isTraceEnabled()) {
            LOG.trace("Connecting to LDAP server {}:{}, binding with user {}",
                      config.getLdapHost(), config.getLdapPort(), config.getName());
        }

        // this will perform an anonymous bind if there were no system credentials
        final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("ldap-connector-%d").build();
        final SimpleTimeLimiter timeLimiter = new SimpleTimeLimiter(Executors.newSingleThreadExecutor(threadFactory));
        @SuppressWarnings("unchecked")
        final Callable<Boolean> timeLimitedConnection = timeLimiter.newProxy(
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return connection.connect();
                    }
                }, Callable.class,
                connectionTimeout, TimeUnit.MILLISECONDS);
        try {
            final Boolean connected = timeLimitedConnection.call();
            if (!connected) {
                return null;
            }
        } catch (UncheckedTimeoutException e) {
            LOG.error("Timed out connecting to LDAP server", e);
            throw new LdapException("Could not connect to LDAP server", e.getCause());
        } catch (LdapException e) {
            throw e;
        } catch (Exception e) {
            // unhandled different exception, should really not happen here.
            throw new LdapException("Unexpected error connecting to LDAP", e);
        }
        connection.bind();

        return connection;
    }

    public LdapEntry search(LdapNetworkConnection connection, String searchBase, String searchPattern, String principal, boolean activeDirectory) throws LdapException, CursorException {
        final LdapEntry ldapEntry = new LdapEntry();

        final String filter = MessageFormat.format(searchPattern, sanitizePrincipal(principal));
        if (LOG.isTraceEnabled()) {
            LOG.trace("Search {} for {}, starting at {}",
                      activeDirectory ? "ActiveDirectory" : "LDAP", filter, searchBase);
        }
        final EntryCursor entryCursor = connection.search(searchBase,
                                                          filter,
                                                          SearchScope.SUBTREE,
                                                          "*");
        final Iterator<Entry> it = entryCursor.iterator();
        if (it.hasNext()) {
            final Entry e = it.next();
            // for generic LDAP use the dn of the entry for the subsequent bind, active directory needs the userPrincipalName attribute (set below)
            if (!activeDirectory) {
                ldapEntry.setDn(e.getDn().getName());
            }

            for (Attribute attribute : e.getAttributes()) {
                if (activeDirectory && attribute.getId().equalsIgnoreCase("userPrincipalName")) {
                    ldapEntry.setDn(attribute.getString());
                }
                if (attribute.isHumanReadable()) {
                    ldapEntry.put(attribute.getId(), attribute.getString());
                }
            }
        } else {
            LOG.trace("No LDAP entry found for filter {}", filter);
            return null;
        }
        LOG.trace("LDAP search found entry for DN {} with search filter {}", ldapEntry.getDn(), filter);
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
                    s += String.format("\\%02x", b);
                }
            }
        }

        return s;
    }

    public boolean authenticate(LdapNetworkConnection connection, String principal, String credentials) throws LdapException {
        final BindRequestImpl bindRequest = new BindRequestImpl();
        bindRequest.setName(principal);
        bindRequest.setCredentials(credentials);
        LOG.trace("Re-binding with DN {} using password", principal);
        final BindResponse bind = connection.bind(bindRequest);
        if (!bind.getLdapResult().getResultCode().equals(ResultCodeEnum.SUCCESS)) {
            LOG.trace("Re-binding DN {} failed", principal);
            throw new RuntimeException(bind.toString());
        }
        LOG.trace("Binding DN {} did not throw, connection authenticated: {}", principal, connection.isAuthenticated());
        return connection.isAuthenticated();
    }
}
