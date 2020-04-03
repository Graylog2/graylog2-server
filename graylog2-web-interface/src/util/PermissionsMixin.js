const _isWildCard = (permissionSet) => (permissionSet.indexOf('*') > -1);
const _permissionPredicate = (permissionSet, p) => {
  if ((permissionSet.indexOf(p) > -1) || (permissionSet.indexOf('*') > -1)) {
    return true;
  }

  const permissionParts = p.split(':');
  if (permissionParts.length >= 2) {
    const first = permissionParts[0];
    const second = `${permissionParts[0]}:${permissionParts[1]}`;
    return (permissionSet.indexOf(first) > -1)
      || (permissionSet.indexOf(`${first}:*`) > -1)
      || (permissionSet.indexOf(second) > -1)
      || (permissionSet.indexOf(`${second}:*`) > -1);
  }
  return (permissionSet.indexOf(`${p}:*`) > -1);
};

export const isPermitted = (possessedPermissions, requiredPermissions) => {
  if (!requiredPermissions || requiredPermissions.length === 0) {
    return true;
  }
  if (!possessedPermissions) {
    return false;
  }
  if (_isWildCard(possessedPermissions)) {
    return true;
  }
  if (requiredPermissions.every) {
    return requiredPermissions.every((p) => _permissionPredicate(possessedPermissions, p));
  }
  return _permissionPredicate(possessedPermissions, requiredPermissions);
};

export const isAnyPermitted = (possessedPermissions, requiredPermissions) => {
  if (!requiredPermissions || requiredPermissions.length === 0) {
    return true;
  }
  if (!possessedPermissions) {
    return false;
  }
  if (_isWildCard(possessedPermissions)) {
    return true;
  }
  return requiredPermissions.some((p) => _permissionPredicate(possessedPermissions, p));
};

const PermissionsMixin = { isPermitted, isAnyPermitted };

export default PermissionsMixin;
