'use strict';

var PermissionsMixin = {
    _isWildCard: function(permissionSet) {
        return (permissionSet.indexOf("*") > -1);
    },

    _permissionPredicate: function(permissionSet, p) {
        if (p.split(":").length === 3) {
            return (permissionSet.indexOf(p) > -1) || (permissionSet.indexOf(p.split(":").slice(0,2).join(":") + ":*") > -1);
        } else {
            return (permissionSet.indexOf(p) > -1) || (permissionSet.indexOf(p + ":*") > -1);
        }
    },

    isPermitted: function(permissionSet, permissions) {
        if (this._isWildCard(permissionSet)) {
            return true;
        }
        var result = permissions.every((p) => this._permissionPredicate(permissionSet, p));
        return result;
    },

    isAnyPermitted: function(permissionSet, permissions) {
        if (this._isWildCard(permissionSet)) {
            return true;
        }
        var result = permissions.some((p) => this._permissionPredicate(permissionSet, p)
        );
        return result;
    }
};

module.exports = PermissionsMixin;
