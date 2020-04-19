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
package org.graylog2.bindings.providers;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.Authenticator;
import org.apache.shiro.authc.pam.FirstSuccessfulStrategy;
import org.apache.shiro.authc.pam.ModularRealmAuthenticator;
import org.apache.shiro.authz.ModularRealmAuthorizer;
import org.apache.shiro.cache.MemoryConstrainedCacheManager;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.DefaultSessionStorageEvaluator;
import org.apache.shiro.mgt.DefaultSubjectDAO;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.session.mgt.DefaultSessionManager;
import org.apache.shiro.subject.Subject;
import org.graylog2.security.InMemoryRolePermissionResolver;
import org.graylog2.security.MongoDbSessionDAO;
import org.graylog2.security.OrderedAuthenticatingRealms;
import org.graylog2.security.realm.MongoDbAuthorizationRealm;
import org.graylog2.security.realm.RootAccountRealm;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class DefaultSecurityManagerProvider implements Provider<DefaultSecurityManager> {
    private DefaultSecurityManager sm = null;

    @Inject
    public DefaultSecurityManagerProvider(MongoDbSessionDAO mongoDbSessionDAO,
                                          MongoDbAuthorizationRealm mongoDbAuthorizationRealm,
                                          Map<String, AuthenticatingRealm> pluggableRealms,
                                          InMemoryRolePermissionResolver inMemoryRolePermissionResolver,
                                          OrderedAuthenticatingRealms orderedAuthenticatingRealms) {
        sm = new DefaultSecurityManager(orderedAuthenticatingRealms);
        final Authenticator authenticator = sm.getAuthenticator();
        if (authenticator instanceof ModularRealmAuthenticator) {
            FirstSuccessfulStrategy strategy = new FirstSuccessfulStrategy();
            strategy.setStopAfterFirstSuccess(true);
            ((ModularRealmAuthenticator) authenticator).setAuthenticationStrategy(strategy);
        }

        // root account realm might be deactivated and won't be present in that case
        List<Realm> realms = Stream.of(mongoDbAuthorizationRealm, pluggableRealms.get(RootAccountRealm.NAME))
                                                  .filter(Objects::nonNull)
                                                  .collect(Collectors.toList());
        final ModularRealmAuthorizer authorizer = new ModularRealmAuthorizer(realms);

        authorizer.setRolePermissionResolver(inMemoryRolePermissionResolver);
        sm.setAuthorizer(authorizer);

        final DefaultSubjectDAO subjectDAO = new DefaultSubjectDAO();
        final DefaultSessionStorageEvaluator sessionStorageEvaluator = new DefaultSessionStorageEvaluator() {
            @Override
            public boolean isSessionStorageEnabled(Subject subject) {
                // save to session if we already have a session. do not create on just for saving the subject
                return subject.getSession(false) != null;
            }
        };
        sessionStorageEvaluator.setSessionStorageEnabled(false);
        subjectDAO.setSessionStorageEvaluator(sessionStorageEvaluator);
        sm.setSubjectDAO(subjectDAO);

        final DefaultSessionManager defaultSessionManager = (DefaultSessionManager) sm.getSessionManager();
        defaultSessionManager.setSessionDAO(mongoDbSessionDAO);
        defaultSessionManager.setDeleteInvalidSessions(true);
        defaultSessionManager.setSessionValidationInterval(TimeUnit.MINUTES.toMillis(5));
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
