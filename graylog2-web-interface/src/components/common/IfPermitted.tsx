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
import type * as React from 'react';
import type { Permissions, Permission } from 'graylog-web-plugin/plugin';

import usePermissions from 'hooks/usePermissions';

/**
 * Wrapper component that renders its children only if the current user fulfills certain permissions.
 * Current user's permissions are fetched from the server.
 */

type Props = {
  children: React.ReactNode;
  permissions: Permissions;
  anyPermissions?: boolean;
};

const _checkPermissions = (
  permissions: Permissions,
  anyPermissions: boolean,
  isPermitted: (permission: Permission) => boolean,
) => {
  if (Array.isArray(permissions)) {
    return anyPermissions ? permissions.find(isPermitted) : permissions.every(isPermitted);
  }

  return isPermitted(permissions);
};

const IfPermitted = ({ children, permissions, anyPermissions = false }: Props) => {
  const { isPermitted } = usePermissions();

  return !permissions || permissions.length === 0 || _checkPermissions(permissions, anyPermissions, isPermitted)
    ? children
    : null;
};

/** @component */
export default IfPermitted;
