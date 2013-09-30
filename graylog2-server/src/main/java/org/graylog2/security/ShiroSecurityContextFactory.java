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
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.Subject;
import org.graylog2.Configuration;
import org.graylog2.Core;
import org.graylog2.jersey.container.netty.SecurityContextFactory;
import org.graylog2.security.realm.GraylogSimpleAccountRealm;
import org.graylog2.security.realm.MongoDbRealm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.SecurityContext;

/**
 * @author Kay Roepke <kay@torch.sh>
 */
public class ShiroSecurityContextFactory implements SecurityContextFactory {
    private static final Logger log = LoggerFactory.getLogger(ShiroSecurityContextFactory.class);
    private org.apache.shiro.mgt.SecurityManager sm;

    public ShiroSecurityContextFactory(Core core) {
        final GraylogSimpleAccountRealm inMemoryRealm = new GraylogSimpleAccountRealm();
        inMemoryRealm.setCachingEnabled(false);
        final Configuration config = core.getConfiguration();
        inMemoryRealm.addRootAccount(
                config.getRootUsername(),
                config.getRootPasswordSha2()
        );

        final MongoDbRealm mongoDbRealm = new MongoDbRealm(core);
        mongoDbRealm.setCredentialsMatcher(new HashedCredentialsMatcher("SHA-1"));
        mongoDbRealm.setCachingEnabled(false);

        sm = new DefaultSecurityManager(Lists.<Realm>newArrayList(mongoDbRealm, inMemoryRealm));
        SecurityUtils.setSecurityManager(sm);
    }
    @Override
    public SecurityContext create(String userName, String credential, boolean isSecure, String authcScheme, String host) {

        return new ShiroSecurityContext(
                new Subject.Builder(sm).host(host).sessionCreationEnabled(false).buildSubject(),
                new UsernamePasswordToken(userName, credential, host),
                isSecure,
                authcScheme
        );
    }
}
