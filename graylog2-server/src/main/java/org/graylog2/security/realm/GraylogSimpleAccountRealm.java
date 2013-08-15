package org.graylog2.security.realm;

import org.apache.shiro.authc.SimpleAccount;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.AllPermission;
import org.apache.shiro.realm.SimpleAccountRealm;
import org.apache.shiro.util.CollectionUtils;

public class GraylogSimpleAccountRealm extends SimpleAccountRealm {

    public GraylogSimpleAccountRealm() {
        super("graylog2-in-memory-realm");
    }

    public void addRootAccount(String username, String password) {
        add(new SimpleAccount(
                username,
                password,
                getName(),
                CollectionUtils.asSet("root"),
                CollectionUtils.<Permission>asSet(new AllPermission())
        ));
    }
}
