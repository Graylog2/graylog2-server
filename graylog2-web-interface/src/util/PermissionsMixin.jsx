const PermissionsMixin = {
  _isWildCard(permissionSet) {
    return (permissionSet.indexOf('*') > -1);
  },

  _permissionPredicate(permissionSet, p) {
    if (p.split(':').length === 3) {
      // eslint-disable-next-line prefer-template
      return (permissionSet.indexOf(p) > -1) || (permissionSet.indexOf(p.split(':').slice(0, 2).join(':') + ':*') > -1);
    }
    return (permissionSet.indexOf(p) > -1) || (permissionSet.indexOf(`${p}:*`) > -1);
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
