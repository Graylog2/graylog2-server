'use strict';

var PermissionsMixin = {
    isPermitted: function(permissionSet, permissions) {
        var result = permissions.every((p) => permissionSet[p]);
        return result;
    }
};

module.exports = PermissionsMixin;
