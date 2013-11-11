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

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import org.apache.shiro.realm.ldap.JndiLdapContextFactory;
import org.graylog2.rest.resources.system.requests.LdapTestLoginRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class LdapConnector {
    private static final Logger log = LoggerFactory.getLogger(LdapConnector.class);

    public Map<String, String> checkCredentials(
            LdapSettings settings,
            String principal,
            String credential) {
        try {
            final Map<String, String> entry = loadAccount(
                    settings.getUri(),
                    settings.getSystemUserName(),
                    settings.getSystemPassword(),
                    settings.getSearchBase(),
                    settings.getPrincipalSearchPattern(),
                    principal);
            return entry;
        } catch (NamingException e) {
            log.info("Unable to load account from LDAP server", e);
            return null;
        }
    }

    public Map<String, String> loadAccount(
            URI ldapUri, String username, String password,
            String searchBase,
            String principalSearchPattern,
            String principal) throws NamingException {

        LdapContext context = connect(ldapUri, username, password);
        return searchPrincipal(context, searchBase, principalSearchPattern, principal);
    }

    public LdapContext connect(URI ldapUri, String username, String password) throws NamingException {
        JndiLdapContextFactory defaultFactory = new JndiLdapContextFactory();
        defaultFactory.setUrl(ldapUri.toString());
        defaultFactory.setSystemUsername(username);
        defaultFactory.setSystemPassword(password);
        try {
            return defaultFactory.getSystemLdapContext();
        } catch (NamingException e) {
            log.error("Unable to connect to LDAP server {} with username {} using password: {}",
                    new Object[] {ldapUri.toString(), username, password != null});
            throw e;
        }
    }

    public Map<String, String> searchPrincipal(
            LdapContext context,
            String searchBase,
            String principalSearchPattern,
            String principal) throws NamingException {

        final SearchControls cons = new SearchControls();
        cons.setSearchScope(SearchControls.SUBTREE_SCOPE);

        final NamingEnumeration<SearchResult> results;
        results = context.search(searchBase, principalSearchPattern, new Object[]{ principal }, cons);

        final HashMap<String,String> entry = Maps.newHashMap();
        if (results.hasMore()) {
            final SearchResult next = results.next();
            final Attributes attrs = next.getAttributes();
            final NamingEnumeration<String> iDs = attrs.getIDs();
            while (iDs.hasMore()) {
                final String key = iDs.next();
                final Object val = attrs.get(key).get();
                final String stringVal;
                if (val instanceof byte[]) {
                    stringVal = new String((byte[]) val, Charsets.UTF_8);
                } else {
                    stringVal = val.toString();
                }
                entry.put(key, stringVal);
            }
        }
        return entry;
    }

    public Map<String, String> testLogin(LdapTestLoginRequest request) throws NamingException {
        JndiLdapContextFactory defaultFactory = new JndiLdapContextFactory();
        defaultFactory.setUrl(request.ldapUri.toString());
        defaultFactory.setSystemUsername(request.systemUsername);
        defaultFactory.setSystemPassword(request.systemPassword);
        defaultFactory.setPoolingEnabled(false);

        final HashMap<String,String> attributes = Maps.newHashMap();
        final LdapContext context = defaultFactory.getSystemLdapContext();

        final SearchControls cons = new SearchControls();
        cons.setSearchScope(SearchControls.SUBTREE_SCOPE);

        final NamingEnumeration<SearchResult> results;
        results = context.search(request.searchBase, request.principalSearchPattern, new Object[]{request.testUsername}, cons);

        if (results.hasMore()) {
            final SearchResult next = results.next();
            final Attributes attrs = next.getAttributes();
            final NamingEnumeration<String> iDs = attrs.getIDs();
            while (iDs.hasMore()) {
                final String key = iDs.next();
                if (key.equals("userPassword")) continue;
                attributes.put(key, attrs.get(key).get().toString());
            }
        }
        context.close();

        return attributes;
    }

}
