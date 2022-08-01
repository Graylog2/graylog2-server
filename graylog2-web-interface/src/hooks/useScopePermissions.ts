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
