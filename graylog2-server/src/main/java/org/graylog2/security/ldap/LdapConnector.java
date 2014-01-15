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
package org.graylog2.security.ldap;

import com.google.common.util.concurrent.SimpleTimeLimiter;
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
import org.graylog2.Core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LdapConnector {
    private static final Logger log = LoggerFactory.getLogger(LdapConnector.class);

    private final Core core;

    public LdapConnector(Core core) {
        this.core = core;
    }

    public LdapNetworkConnection connect(LdapConnectionConfig config) throws LdapException {
        final LdapNetworkConnection connection = new LdapNetworkConnection(config);
        connection.setTimeOut(TimeUnit.SECONDS.toMillis(2)); // TODO timeout value

        if (log.isTraceEnabled()) {
            log.trace("Connecting to LDAP server {}:{}, binding with user {}",
                      new Object[]{config.getLdapHost(), config.getLdapPort(), config.getName()});
        }
        // this will perform an anonymous bind if there were no system credentials
        final SimpleTimeLimiter timeLimiter = new SimpleTimeLimiter(Executors.newSingleThreadExecutor());
        @SuppressWarnings("unchecked")
        final Callable<Boolean> timeLimitedConnection = timeLimiter.newProxy(
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return connection.connect();
                    }
                }, Callable.class,
                2, TimeUnit.SECONDS); // TODO timeout value
        try {
            final Boolean connected = timeLimitedConnection.call();
            if (!connected) {
                return null;
            }
        } catch (UncheckedTimeoutException e) {
            log.error("Timed out connecting to LDAP server", e);
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

        final String filter = MessageFormat.format(searchPattern, principal);
        if (log.isTraceEnabled()) {
            log.trace("Search {} for {}, starting at {}",
                      new Object[]{activeDirectory ? "ActiveDirectory" : "LDAP", filter, searchBase});
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
            log.trace("No LDAP entry found for filter {}", filter);
            return null;
        }
        log.trace("LDAP search found entry for DN {} with search filter {}", ldapEntry.getDn(), filter);
        return ldapEntry;
    }

    public boolean authenticate(LdapNetworkConnection connection, String principal, String credentials) throws LdapException {
        final BindRequestImpl bindRequest = new BindRequestImpl();
        bindRequest.setName(principal);
        bindRequest.setCredentials(credentials);
        log.trace("Re-binding with DN {} using password", principal);
        final BindResponse bind = connection.bind(bindRequest);
        if (!bind.getLdapResult().getResultCode().equals(ResultCodeEnum.SUCCESS)) {
            log.trace("Re-binding DN {} failed", principal);
            throw new RuntimeException(bind.toString());
        }
        log.trace("Binding DN {} did not throw, connection authenticated: {}", principal, connection.isAuthenticated());
        return connection.isAuthenticated();
    }
}
