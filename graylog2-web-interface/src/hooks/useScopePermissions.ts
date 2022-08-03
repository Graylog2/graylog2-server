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

import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';

interface ScopeParams {
  is_mutable: boolean;
}

type ScopeName = 'DEFAULT' | 'ILLUMINATE';

type EntityScopeRecord = Record<ScopeName, ScopeParams>;

type EntityScopeType = {
  entity_scopes: EntityScopeRecord,
};

const fetchScopePermissions: () => Promise<EntityScopeType> = () => {
  return fetch('GET', qualifyUrl(ApiRoutes.EntityScopeController.getScope().url));
};

const useGetPermissionsByScope = (inScope: string) => {
  const { data, isLoading } = useQuery(
    ['scope-permissions'],
    fetchScopePermissions,
    {
      onError: (fetchError: Error) => {
        UserNotification.error(`Error fetching entity scope permissions: ${fetchError.message}`);
      },
      retry: 1,
      cacheTime: 1000 * 60 * 60 * 3, // cache for 3 hours
      staleTime: 1000 * 60 * 60 * 3, // data is valid for 3 hours
    },
  );

  const scope = inScope ? inScope.toUpperCase() : 'DEFAULT';
  const permissions: ScopeParams = isLoading ? { is_mutable: false } : data.entity_scopes[scope];

  return {
    isLoading,
    data: permissions,
  };
};

export default useGetPermissionsByScope;
