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
import { useQuery } from 'react-query';

import EntityScopesPermissions from 'logic/permissions/ScopePermissions';
import UserNotification from 'util/UserNotification';
import type { GenericEntityType } from 'logic/lookup-tables/types';

const useGetPermissionsByScope = () => {
  const { data, isLoading, isError, error } = useQuery<any, Error>(
    'scope-permissions',
    EntityScopesPermissions.get,
    {
      retry: 1,
      cacheTime: 1000 * 60 * 60 * 3, // cache for 3 hours
      staleTime: 1000 * 60 * 60 * 3, // data is valid for 3 hours
    },
  );

  if (isError && error) UserNotification.error(error.message);

  const getScopePermissions = (entity: GenericEntityType) => {
    const scope = entity.scope ? entity.scope.toUpperCase() : 'DEFAULT';

    return isLoading ? {} : data.entity_scopes[scope];
  };

  return { getScopePermissions };
};

export default useGetPermissionsByScope;
