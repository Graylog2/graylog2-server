// NOTE: Mock method to be able to move forward with tests. Remove after API
// defined how are we getting the permissions to show and hide actions.
const SCOPE_PERMISSIONS = {
  ILLUMINATE: { is_mutable: false },
  DEFAULT: { is_mutable: true },
};

const useGetPermissionsByScope = () => {
  const getScopePermissions = (inScope: string) => {
    const scope = inScope ? inScope.toUpperCase() : 'DEFAULT';

    return SCOPE_PERMISSIONS[scope];
  };

  return { getScopePermissions };
};

export default useGetPermissionsByScope;
