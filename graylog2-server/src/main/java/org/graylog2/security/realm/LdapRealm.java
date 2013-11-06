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
package org.graylog2.security.realm;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.apache.shiro.realm.ldap.AbstractLdapRealm;
import org.apache.shiro.realm.ldap.LdapContextFactory;
import org.apache.shiro.subject.PrincipalCollection;
import org.graylog2.Core;
import org.graylog2.database.ValidationException;
import org.graylog2.security.LdapSettings;
import org.graylog2.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LdapRealm extends AbstractLdapRealm {
    private static final Logger log = LoggerFactory.getLogger(LdapRealm.class);

    private final Core core;

    // we need to protect updating the settings with a rw-mutex because the
    // settings are applied separate from each other and would conflict with the
    // running authentication attempts
    private final ReentrantReadWriteLock settingsUpdateLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = settingsUpdateLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = settingsUpdateLock.writeLock();

    private String principalSearchPattern;
    private String usernameAttribute;

    private boolean enabled = false;

    public LdapRealm(Core core) {
        this.core = core;
        core.setLdapRealm(this);
        final LdapSettings settings = LdapSettings.load(core);

        applySettings(settings);
    }

    public void applySettings(LdapSettings settings) {
        writeLock.lock();
        try {
            if (settings == null) {
                log.info("LDAP settings are empty, turning LDAP support off.");
                enabled = false;
                return;
            }

            final String systemUserName = settings.getSystemUserName();
            if (systemUserName != null) {
                setSystemUsername(systemUserName);
            }
            final String password = settings.getSystemPassword();
            if (password != null) {
                setSystemPassword(password);
            }
            setUrl(settings.getUri().toString());
            setSearchBase(settings.getSearchBase());
            principalSearchPattern = settings.getPrincipalSearchPattern();
            usernameAttribute = settings.getUsernameAttribute();
            enabled = settings.isEnabled();
            // TODO validation of principalSearchPattern etc
        } catch (Exception e) {
            // any exception thrown means that we can't use LDAP.
            // most likely it's a malformed URI...
            log.error("Cannot apply LDAP settings. Disabling LDAP entirely.", e);
            enabled = false;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    protected AuthenticationInfo queryForAuthenticationInfo(AuthenticationToken token, LdapContextFactory
        ldapContextFactory) throws NamingException {
        final NamingEnumeration<SearchResult> results;
        readLock.lock();
        try {
            if (!enabled) {
                log.info("LDAP realm is disabled");
                return null;
            }
            final LdapContext ldapContext = ldapContextFactory.getSystemLdapContext();
            final SearchControls cons = new SearchControls();
            cons.setSearchScope(SearchControls.SUBTREE_SCOPE);
            results = ldapContext.search(searchBase, principalSearchPattern, new Object[]{token.getPrincipal()}, cons);
        } finally {
            readLock.unlock();
        }

        // we want to read at most one result
        if (results.hasMore()) {
            final SearchResult result = results.next();
            final Attributes attributes = result.getAttributes();

            // TODO let users configure an objectClass so that we know what to read from ldap.
            // we currently assume inetOrgPerson
            final Object cn = attributes.get("cn").get();
            Attribute rfc822Mailbox = attributes.get("rfc822Mailbox");
            if (rfc822Mailbox == null) {
                rfc822Mailbox = attributes.get("mail");
            }
            final Object mail = rfc822Mailbox.get();
            final byte[] passwordBytes = (byte[])attributes.get("userPassword").get();
            final String userPassword = new String(passwordBytes, Charsets.UTF_8);
            final Object userName = attributes.get(usernameAttribute).get();
            if (userName == null) {
                throw new AuthenticationException("No attribute named " + usernameAttribute + " defined for user " + token.getPrincipal() + ". Cannot authenticate.");
            }

            final String username = userName.toString();
            User user = User.load(username, core);
            if (user == null) {
                user = new User(Maps.<String, Object>newHashMap(), core);
            }
            user.setFullName(cn.toString());
            user.setName(username);
            user.setEmail(mail != null ? mail.toString() : "");
            final String hashedPassword = new SimpleHash("SHA-256", userPassword).toString(); // the web interface sends its password as SHA-256(realpassword)
            final String hashToStore = new SimpleHash("SHA-1", hashedPassword, core.getConfiguration().getPasswordSecret()).toString();
            user.setHashedPassword(hashToStore);
            user.setPermissions(Lists.<String>newArrayList("*")); // TODO groups
            user.setExternal(true); // TODO fix this in respect to isReadOnly. Maybe split "core" part of user data in db?
            try {
                user.save();
            } catch (ValidationException e) {
                log.error("Cannot migrate LDAP user {} to local storage", token.getPrincipal());
                throw new AuthenticationException("Cannot migrate user from LDAP");
            }
            return null;
            //return new SimpleAccount(userName, userPassword, "ldap");
        }

        // no decision, we don't have an LDAP user for that principal.
        // TODO should this fail the entire authentication attempt?
        return null;
    }

    @Override
    protected AuthorizationInfo queryForAuthorizationInfo(PrincipalCollection principal, LdapContextFactory
        ldapContextFactory) throws NamingException {
        return null;
    }
}
