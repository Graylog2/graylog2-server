'use strict';

var PermissionsMixin = {
    isPermitted: function(permissionSet, permissions) {
        if (permissionSet.indexOf("*") > -1) {
            return true;
        }
        var result = permissions.every((p) => {
                if (p.split(":").length === 3) {
                    return (permissionSet.indexOf(p) > -1) || (permissionSet.indexOf(p.split(":").slice(0,2).join(":") + ":*") > -1);
                } else {
                    return (permissionSet.indexOf(p) > -1) || (permissionSet.indexOf(p + ":*") > -1);
                }
            }
        );
        return result;
    }
};

module.exports = PermissionsMixin;
