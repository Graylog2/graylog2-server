import { useQuery } from 'react-query';

import UserNotification from 'util/UserNotification';
import type { GenericEntityType } from 'logic/lookup-tables/types';

// NOTE: Mock method to be able to move forward with tests. Remove after API
// defined how are we getting the permissions to show and hide actions.
const fetchScopePermissions = async () => {
  return new Promise((resolve: any) => {
    setTimeout(() => {
      console.log('data resolved');

      return resolve({
        ILLUMINATE: { is_mutable: false },
        DEFAULT: { is_mutable: true },
      });
    }, 1000);
  });
};

const useGetPermissionsByScope = () => {
  const { data, isLoading, isError, error } = useQuery<any, Error>(
    'scope-permissions',
    fetchScopePermissions,
    {
      retry: 1,
      cacheTime: 1000 * 60 * 60 * 3, // cache for 3 hours
      staleTime: 1000 * 60 * 60 * 3, // data is valid for 3 hours
    },
  );

  if (isError && error) UserNotification.error(error.message);

  const getScopePermissions = (entity: GenericEntityType) => {
    const scope = entity.scope ? entity.scope.toUpperCase() : 'DEFAULT';

    return isLoading ? {} : data[scope];
  };

  return { getScopePermissions };
};

export default useGetPermissionsByScope;
