import { useMemo } from 'react';

import useCurrentUser from 'hooks/useCurrentUser';
import getPermissionPrefixByType from 'util/getPermissionPrefixByType';
import { isPermitted } from 'util/PermissionsMixin';
import { getValuesFromGRN } from 'logic/permissions/GRN';

const useHasEntityPermissionByGRN = (grn: string, permissionType: string = 'read') => {
  const { id, type } = getValuesFromGRN(grn);
  const { permissions } = useCurrentUser();

  return useMemo(() => isPermitted(permissions, `${getPermissionPrefixByType(type)}${permissionType}:${id}`), [id, permissionType, permissions, type]);
};

export default useHasEntityPermissionByGRN;
