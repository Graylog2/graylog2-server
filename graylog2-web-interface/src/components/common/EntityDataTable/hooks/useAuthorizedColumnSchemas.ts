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

import { useMemo } from 'react';

import type { ColumnSchema } from 'components/common/EntityDataTable';
import useCurrentUser from 'hooks/useCurrentUser';
import { isAnyPermitted, isPermitted } from 'util/PermissionsMixin';

const useAuthorizedColumnSchemas = (columnSchemas: Array<ColumnSchema>) => {
  const currentUser = useCurrentUser();

  return useMemo(
    () =>
      columnSchemas.filter(({ permissions, anyPermissions, hidden }) => {
        if (hidden) {
          return false;
        }

        if (permissions?.length) {
          return anyPermissions
            ? isAnyPermitted(currentUser.permissions, permissions)
            : isPermitted(currentUser.permissions, permissions);
        }

        return true;
      }),
    [columnSchemas, currentUser.permissions],
  );
};

export default useAuthorizedColumnSchemas;
