// @flow strict
import Reflux from 'reflux';

import type { PaginatedList } from 'stores/roles/AuthzRolesStore';
import { singletonActions } from 'views/logic/singleton';
import type { RefluxActions } from 'stores/StoreTypes';

type AuthzRolesActionsType = RefluxActions<{
  loadForUser: (username: string,
                page: number,
                perPage: number,
                query: string) => Promise<PaginatedList>,
}>;

const AuthzRolesActions: AuthzRolesActionsType = singletonActions(
  'AuthzRoles',
  () => Reflux.createActions({
    loadForuser: { asyncResult: true },
  }),
);

export default AuthzRolesActions;
