package org.graylog.security.permissions;

import org.apache.shiro.authz.permission.WildcardPermission;

public class CaseSensitiveWildcardPermission extends WildcardPermission {
    public CaseSensitiveWildcardPermission(String wildcardString) {
        super(wildcardString, true);
    }
}
