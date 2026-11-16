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
import * as React from 'react';
import type { Permission } from 'graylog-web-plugin/plugin';

import usePermissions from 'hooks/usePermissions';
import UnauthorizedErrorPage from 'pages/UnauthorizedErrorPage';

type Props = {
  children: React.ReactElement;
  displayPageLayout?: boolean;
  permissions?: Permission | Array<Permission>;
};

const RoutePermissionGuard = ({ children, displayPageLayout = true, permissions = undefined }: Props) => {
  const { isPermitted } = usePermissions();

  if (permissions && !isPermitted(permissions)) {
    const requiredPermissions = Array.isArray(permissions) ? permissions.join(', ') : permissions;

    return (
      <UnauthorizedErrorPage
        displayPageLayout={displayPageLayout}
        errorDetails={`Required permissions: ${requiredPermissions}.`}
      />
    );
  }

  return children;
};

export default RoutePermissionGuard;
