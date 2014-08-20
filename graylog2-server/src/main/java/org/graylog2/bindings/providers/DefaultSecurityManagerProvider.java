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
package org.graylog2.bindings.providers;

import com.google.common.collect.Lists;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.Authenticator;
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
import org.graylog2.security.MongoDbSessionDAO;
import org.graylog2.security.realm.*;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class DefaultSecurityManagerProvider implements Provider<DefaultSecurityManager> {
    private static DefaultSecurityManager sm = null;

    @Inject
    public DefaultSecurityManagerProvider(MongoDbSessionDAO mongoDbSessionDAO,
                                          PasswordAuthenticator passwordAuthenticator,
                                          MongoDbAuthorizationRealm mongoDbAuthorizationRealm,
                                          LdapUserAuthenticator ldapUserAuthenticator,
                                          SessionAuthenticator sessionAuthenticator,
                                          AccessTokenAuthenticator accessTokenAuthenticator,
                                          Configuration configuration) {
        final GraylogSimpleAccountRealm inMemoryRealm = new GraylogSimpleAccountRealm();
        inMemoryRealm.setCachingEnabled(false);
        inMemoryRealm.addRootAccount(
                configuration.getRootUsername(),
                configuration.getRootPasswordSha2()
        );
        inMemoryRealm.setCredentialsMatcher(new HashedCredentialsMatcher("SHA-256"));

        passwordAuthenticator.setCachingEnabled(false);
        passwordAuthenticator.setCredentialsMatcher(new HashedCredentialsMatcher("SHA-1"));
        mongoDbAuthorizationRealm.setCachingEnabled(false);

        ldapUserAuthenticator.setCachingEnabled(false);

        sessionAuthenticator.setCachingEnabled(false);
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
        defaultSessionManager.setSessionDAO(mongoDbSessionDAO);
        defaultSessionManager.setDeleteInvalidSessions(true);
        defaultSessionManager.setCacheManager(new MemoryConstrainedCacheManager());
        // DO NOT USE global session timeout!!! It's fucky.
        //defaultSessionManager.setGlobalSessionTimeout(TimeUnit.SECONDS.toMillis(5));

        SecurityUtils.setSecurityManager(sm);
    }

    @Override
    public DefaultSecurityManager get() {
        return sm;
    }
}
