const PermissionsMixin = {
  _isWildCard(permissionSet) {
    return (permissionSet.indexOf('*') > -1);
  },

  _permissionPredicate(permissionSet, p) {
      if ((permissionSet.indexOf(p) > -1) || (permissionSet.indexOf('*') > -1)) {
      return true;
    }

    let permissionParts = p.split(':');
    if (permissionParts.length >= 2) {
      let first = permissionParts[0];
      let second = permissionParts[0] + ':' + permissionParts[1];
      return (permissionSet.indexOf(first) > -1)
        || (permissionSet.indexOf(first + ':*') > -1)
        || (permissionSet.indexOf(second) > -1)
        || (permissionSet.indexOf(second + ':*') > -1);
    }
    return (permissionSet.indexOf(`${p}:*`) > -1);
  },

  isPermitted(permissionSet, permissions) {
    if (this._isWildCard(permissionSet)) {
      return true;
    }
    if (permissions.every) {
      return permissions.every(p => this._permissionPredicate(permissionSet, p));
    }
    return this._permissionPredicate(permissionSet, permissions);
  },

  isAnyPermitted(permissionSet, permissions) {
    if (this._isWildCard(permissionSet)) {
      return true;
    }
    return permissions.some(p => this._permissionPredicate(permissionSet, p));
  },
};

export default PermissionsMixin;
