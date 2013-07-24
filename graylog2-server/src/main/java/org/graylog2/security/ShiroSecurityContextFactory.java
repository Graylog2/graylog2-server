package org.graylog2.security;

import com.google.common.collect.Lists;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.realm.SimpleAccountRealm;
import org.apache.shiro.subject.Subject;
import org.graylog2.Core;
import org.graylog2.jersey.container.netty.SecurityContextFactory;
import org.graylog2.security.realm.MongoDbRealm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.SecurityContext;

public class ShiroSecurityContextFactory implements SecurityContextFactory {
    private static final Logger log = LoggerFactory.getLogger(ShiroSecurityContextFactory.class);
    private org.apache.shiro.mgt.SecurityManager sm;

    public ShiroSecurityContextFactory(Core core) {
        final SimpleAccountRealm inMemoryRealm = new SimpleAccountRealm();
        inMemoryRealm.setCachingEnabled(false);
        inMemoryRealm.addAccount("kay", "pass", "admin");

        final MongoDbRealm mongoDbRealm = new MongoDbRealm(core);
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
