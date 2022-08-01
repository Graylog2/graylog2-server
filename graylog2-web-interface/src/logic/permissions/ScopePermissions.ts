import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';

const EntityScopesPermissions = {
  get: async () => {
    return fetch('GET', qualifyUrl(ApiRoutes.EntityShareController.entityScopes().url));
  },
};

export default EntityScopesPermissions;
