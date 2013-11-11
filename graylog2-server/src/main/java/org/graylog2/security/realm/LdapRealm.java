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
import org.graylog2.security.ldap.LdapConnector;
import org.graylog2.security.ldap.LdapSettings;
import org.graylog2.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LdapRealm extends AbstractLdapRealm {
    private static final Logger log = LoggerFactory.getLogger(LdapRealm.class);

    private final Core core;
    public final LdapConnector ldapConnector;

    // we need to protect updating the settings with a rw-mutex because the
    // settings are applied separate from each other and would conflict with the
    // running authentication attempts
    private final ReentrantReadWriteLock settingsUpdateLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = settingsUpdateLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = settingsUpdateLock.writeLock();

    private String principalSearchPattern;
    private String usernameAttribute;

    private boolean enabled = false;

    public LdapRealm(Core core, LdapConnector ldapConnector) {
        this.core = core;
        this.ldapConnector = ldapConnector;
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
    protected AuthenticationInfo queryForAuthenticationInfo(AuthenticationToken token,
            LdapContextFactory ldapContextFactory) throws NamingException {
        final Map<String, String> entry;
        readLock.lock();
        try {
            if (!enabled) {
                log.info("LDAP realm is disabled");
                return null;
            }
            final LdapContext ldapContext = ldapContextFactory.getSystemLdapContext();
            entry = ldapConnector.searchPrincipal(ldapContext,
                                                  searchBase,
                                                  principalSearchPattern,
                                                  token.getPrincipal().toString());
        } finally {
            readLock.unlock();
        }

        if (entry.isEmpty()) {
            log.debug("Unable to find an LDAP entry for user {}", token.getPrincipal());
            return null;
        }

        // TODO let users configure an objectClass so that we know what to read from ldap.
        // we currently assume inetOrgPerson
        final String cn = entry.get("cn");
        String mail = entry.get("rfc822Mailbox");
        if (mail == null) {
            // TODO does this happen, isn't rfc822Mailbox the canonical attribute name?
            mail = entry.get("mail");
        }
        final String userPassword = entry.get("userPassword");
        final String username = entry.get(usernameAttribute);
        if (username == null) {
            throw new AuthenticationException("No attribute named " + usernameAttribute + " defined for user " + token.getPrincipal() + ". Cannot authenticate.");
        }

        // load from mongodb and potentially merge the info we got from ldap
        User user = User.load(username, core);
        if (user == null) {
            user = new User(Maps.<String, Object>newHashMap(), core);
        }
        user.setFullName(cn);
        user.setName(username);
        user.setEmail(mail != null ? mail : "");
        final String hashedPassword = new SimpleHash("SHA-256",
                                                     userPassword).toString(); // the web interface sends its password as SHA-256(realpassword)
        final String hashToStore = new SimpleHash("SHA-1",
                                                  hashedPassword,
                                                  core.getConfiguration().getPasswordSecret()).toString();
        user.setHashedPassword(hashToStore);
        user.setPermissions(Lists.<String>newArrayList("*")); // TODO groups
        user.setExternal(true); // TODO fix this in respect to isReadOnly. Maybe split "core" part of user data in db?
        try {
            user.save();
        } catch (ValidationException e) {
            log.error("Cannot migrate LDAP user {} to local storage", token.getPrincipal());
            throw new AuthenticationException("Cannot migrate user from LDAP");
        }
        // delegate checking to the mongodb realm, which now has the user because we just copied it from ldap
        return null;
    }

    @Override
    protected AuthorizationInfo queryForAuthorizationInfo(PrincipalCollection principal, LdapContextFactory
            ldapContextFactory) throws NamingException {
        return null;
    }

}
