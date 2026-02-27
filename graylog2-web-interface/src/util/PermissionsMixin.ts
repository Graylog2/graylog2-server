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
import type { List } from 'immutable';
import type { Permission } from 'graylog-web-plugin/plugin';

type Permissions = Array<Permission> | List<Permission>;

const _isWildCard = (permissionSet: Permissions) => permissionSet.indexOf('*') > -1;

const _permissionPredicate = (permissionSet: Permissions, p: Permission) => {
  if (permissionSet.indexOf(p) > -1 || permissionSet.indexOf('*') > -1) {
    return true;
  }

  const permissionParts = p.split(':');

  if (permissionParts.length >= 2) {
    const first = permissionParts[0] as Permission;
    const second = `${permissionParts[0]}:${permissionParts[1]}` as Permission;

    return (
      permissionSet.indexOf(first) > -1 ||
      permissionSet.indexOf(`${first}:*` as Permission) > -1 ||
      permissionSet.indexOf(second) > -1 ||
      permissionSet.indexOf(`${second}:*` as Permission) > -1
    );
  }

  return permissionSet.indexOf(`${p}:*` as Permission) > -1;
};

export const isPermitted = (possessedPermissions: Permissions, requiredPermissions: Permissions | Permission) => {
  if (!requiredPermissions || (Array.isArray(requiredPermissions) && requiredPermissions.length === 0)) {
    return true;
  }

  if (!possessedPermissions) {
    return false;
  }

  if (_isWildCard(possessedPermissions)) {
    return true;
  }

  if (typeof requiredPermissions === 'object') {
    return requiredPermissions.every((p) => _permissionPredicate(possessedPermissions, p));
  }

  return _permissionPredicate(possessedPermissions, requiredPermissions);
};

export const isAnyPermitted = (possessedPermissions: Permissions, requiredPermissions: Permissions) => {
  if (!requiredPermissions || (Array.isArray(requiredPermissions) && requiredPermissions.length === 0)) {
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

const ADMIN_PERMISSION = '*';
export const hasAdminPermission = (permissions: Permissions) => permissions.includes(ADMIN_PERMISSION);

const PermissionsMixin = { isPermitted, isAnyPermitted };

export default PermissionsMixin;
