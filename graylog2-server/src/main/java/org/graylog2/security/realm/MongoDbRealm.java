package org.graylog2.security.realm;

import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.graylog2.Core;
import org.graylog2.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoDbRealm extends AuthorizingRealm {
    private static final Logger log = LoggerFactory.getLogger(MongoDbRealm.class);
    private final Core core;

    public MongoDbRealm(Core core) {
        this.core = core;
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        return null;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authToken) throws AuthenticationException {
        if (!(authToken instanceof UsernamePasswordToken)) {
            throw new IllegalArgumentException("Only implemented for UsernamePasswordToken currently.");
        }
        UsernamePasswordToken token = (UsernamePasswordToken) authToken;
        log.info("Retrieving authc info for user {}", token.getUsername());

        final SimpleAccount simpleAccount;
        if (User.exists(token.getUsername(), new String(token.getPassword()), core)) {
            simpleAccount = new SimpleAccount(token.getPrincipal(),
                    token.getCredentials(),
                    ByteSource.Util.bytes(core.getConfiguration().getPasswordSecret()),
                    "graylog2MongoDbRealm");
            log.info("User {} authenticated by hashed password", token.getUsername());
        } else {
            log.warn("User {} could not be authenticated", token.getUsername());
            throw new AuthenticationException("Unknown user or wrong credentials.");
        }

        return simpleAccount;
    }
}
