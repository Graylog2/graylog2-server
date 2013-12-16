/**
 * Copyright 2013 Kay Roepke <kay@torch.sh>
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
 *
 */
package org.graylog2.security;

import com.google.common.collect.Lists;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.Authenticator;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.authc.pam.FirstSuccessfulStrategy;
import org.apache.shiro.authc.pam.ModularRealmAuthenticator;
import org.apache.shiro.authz.ModularRealmAuthorizer;
import org.apache.shiro.cache.MemoryConstrainedCacheManager;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.DefaultSessionStorageEvaluator;
import org.apache.shiro.mgt.DefaultSubjectDAO;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.session.mgt.DefaultSessionManager;
import org.apache.shiro.subject.Subject;
import org.graylog2.Configuration;
import org.graylog2.Core;
import org.graylog2.jersey.container.netty.SecurityContextFactory;
import org.graylog2.security.ldap.LdapConnector;
import org.graylog2.security.realm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.SecurityContext;

public class ShiroSecurityContextFactory implements SecurityContextFactory {
    private static final Logger log = LoggerFactory.getLogger(ShiroSecurityContextFactory.class);
    private DefaultSecurityManager sm;

    public ShiroSecurityContextFactory(Core core) {
        final GraylogSimpleAccountRealm inMemoryRealm = new GraylogSimpleAccountRealm();
        inMemoryRealm.setCachingEnabled(false);
        final Configuration config = core.getConfiguration();
        inMemoryRealm.addRootAccount(
                config.getRootUsername(),
                config.getRootPasswordSha2()
        );
        inMemoryRealm.setCredentialsMatcher(new HashedCredentialsMatcher("SHA-256"));

        final PasswordAuthenticator passwordAuthenticator = new PasswordAuthenticator(core);
        passwordAuthenticator.setCachingEnabled(false);
        passwordAuthenticator.setCredentialsMatcher(new HashedCredentialsMatcher("SHA-1"));
        final MongoDbAuthorizationRealm mongoDbAuthorizationRealm = new MongoDbAuthorizationRealm(core);
        mongoDbAuthorizationRealm.setCachingEnabled(false);

        final LdapConnector ldapConnector = new LdapConnector(core);
        core.setLdapConnector(ldapConnector);
        final LdapUserAuthenticator ldapUserAuthenticator = new LdapUserAuthenticator(core, ldapConnector);
        ldapUserAuthenticator.setCachingEnabled(false);
        core.setLdapAuthenticator(ldapUserAuthenticator);

        final SessionAuthenticator sessionAuthenticator = new SessionAuthenticator(core);
        sessionAuthenticator.setCachingEnabled(false);
        final AccessTokenAuthenticator accessTokenAuthenticator = new AccessTokenAuthenticator(core);
        accessTokenAuthenticator.setCachingEnabled(false);


        sm = new DefaultSecurityManager(Lists.<Realm>newArrayList(
                sessionAuthenticator,
                accessTokenAuthenticator,
                ldapUserAuthenticator,
                passwordAuthenticator,
                inMemoryRealm));
        final Authenticator authenticator = sm.getAuthenticator();
        if (authenticator instanceof ModularRealmAuthenticator) {
            ((ModularRealmAuthenticator) authenticator).setAuthenticationStrategy(new FirstSuccessfulStrategy());
        }
        sm.setAuthorizer(new ModularRealmAuthorizer(Lists.<Realm>newArrayList(mongoDbAuthorizationRealm, inMemoryRealm)));

        final DefaultSubjectDAO subjectDAO = new DefaultSubjectDAO();
        final DefaultSessionStorageEvaluator sessionStorageEvaluator = new DefaultSessionStorageEvaluator() {
            @Override
            public boolean isSessionStorageEnabled(Subject subject) {
                // save to session if we already have a session. do not create on just for saving the subject
                return (subject.getSession(false) != null);
            }
        };
        sessionStorageEvaluator.setSessionStorageEnabled(false);
        subjectDAO.setSessionStorageEvaluator(sessionStorageEvaluator);
        sm.setSubjectDAO(subjectDAO);

        final DefaultSessionManager defaultSessionManager = (DefaultSessionManager) sm.getSessionManager();
        defaultSessionManager.setSessionDAO(new MongoDbSessionDAO(core));
        defaultSessionManager.setDeleteInvalidSessions(true);
        defaultSessionManager.setCacheManager(new MemoryConstrainedCacheManager());
        // DO NOT USE global session timeout!!! It's fucky.
        //defaultSessionManager.setGlobalSessionTimeout(TimeUnit.SECONDS.toMillis(5));
        core.setSecurityManager(sm);

        SecurityUtils.setSecurityManager(sm);
    }

    @Override
    public SecurityContext create(String userName, String credential, boolean isSecure, String authcScheme, String host) {

        AuthenticationToken authToken;
        if (credential == null) {
            authToken = new UsernamePasswordToken(userName, credential, host);
        } else {
            if (credential.equalsIgnoreCase("session")) {
                authToken = new SessionIdToken(userName, host);
            } else if (credential.equalsIgnoreCase("token")) {
                authToken = new AccessTokenAuthToken(userName, host);
            } else {
                authToken = new UsernamePasswordToken(userName, credential, host);
            }
        }

        return new ShiroSecurityContext(
                new Subject.Builder(sm).host(host).sessionCreationEnabled(false).buildSubject(),
                authToken,
                isSecure,
                authcScheme
        );
    }
}
