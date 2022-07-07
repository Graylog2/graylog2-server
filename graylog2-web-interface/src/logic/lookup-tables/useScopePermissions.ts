// NOTE: Mock method to be able to move forward with tests. Remove after API
// defined how are we getting the permissions to show and hide actions.

const useGetPermissionsByScope = () => {
  const getPermissionsByScope = (scope: string) => {
    switch (scope) {
      case 'ILLUMINATE':
        return { edit: false, delete: false };
      default:
        return { edit: true, delete: true };
    }
  };

  return { getPermissionsByScope };
};

export default useGetPermissionsByScope;
