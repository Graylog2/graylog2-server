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

import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import UserNotification from 'util/UserNotification';

// NOTE: Mock method to be able to move forward with tests. Remove after API
// defined how are we getting the permissions to show and hide actions.
// const fetchScopePermissions = async () => {
//   return new Promise((resolve: any) => {
//     setTimeout(() => {
//       console.log('data resolved');

//       return resolve({
//         ILLUMINATE: { is_mutable: false },
//         DEFAULT: { is_mutable: true },
//       });
//     }, 1000);
//   });
// };

type EntityScopeType = {
  scope_properties: {
    default: {
      is_mutable: boolean,
    },
    illuminate: {
      is_mutable: boolean,
    },
  }
};

const fetchScopePermissions = async () => {
  try {
    const data = await fetch('GET', qualifyUrl(ApiRoutes.EntityScopeController.getScope().url));

    return data;
  } catch (e) {
    return UserNotification.error('Could not fetch entity scopes.');
  }
};

const useGetPermissionsByScope = () => {
  const { data, isLoading, isError, error } = useQuery<EntityScopeType, Error>(
    'scope-permissions',
    fetchScopePermissions,
    {
      retry: 1,
      cacheTime: 1000 * 60 * 60 * 3, // cache for 3 hours
      staleTime: 1000 * 60 * 60 * 3, // data is valid for 3 hours
    },
  );

  if (isError && error) UserNotification.error(error.message);

  const getScopePermissions = (inScope: string) => {
    const scope = inScope ? inScope.toUpperCase() : 'DEFAULT';

    return isLoading ? {} : data.scope_properties[scope];
  };

  return { getScopePermissions };
};

export default useGetPermissionsByScope;
