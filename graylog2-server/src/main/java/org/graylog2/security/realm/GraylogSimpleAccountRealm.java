package org.graylog2.security.realm;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAccount;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.AllPermission;
import org.apache.shiro.realm.SimpleAccountRealm;
import org.apache.shiro.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraylogSimpleAccountRealm extends SimpleAccountRealm {
    private static final Logger log = LoggerFactory.getLogger(GraylogSimpleAccountRealm.class);

    public GraylogSimpleAccountRealm() {
        super("graylog2-in-memory-realm");
    }

    public void addRootAccount(String username, String password) {
        log.debug("Adding root account named {}, having all permissions", username);
        add(new SimpleAccount(
                username,
                password,
                getName(),
                CollectionUtils.asSet("root"),
                CollectionUtils.<Permission>asSet(new AllPermission())
        ));
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        log.debug("Retrieving authentication info for user {}", token.getPrincipal());
        return super.doGetAuthenticationInfo(token);
    }
}
