const PermissionsMixin = {
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
    if (permissions.every) {
      return permissions.every((p) => this._permissionPredicate(permissionSet, p));
    }
    return this._permissionPredicate(permissionSet, permissions);
  },

  isAnyPermitted: function(permissionSet, permissions) {
    if (this._isWildCard(permissionSet)) {
      return true;
     }
    const result = permissions.some((p) => this._permissionPredicate(permissionSet, p));
    return result;
  }
};

export default PermissionsMixin;
