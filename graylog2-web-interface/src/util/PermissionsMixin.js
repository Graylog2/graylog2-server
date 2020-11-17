/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
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
