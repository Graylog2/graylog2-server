// @flow strict
import Reflux from 'reflux';

import type { PaginatedListType } from 'stores/roles/AuthzRolesStore';
import { singletonActions } from 'views/logic/singleton';
import type { RefluxActions } from 'stores/StoreTypes';

type AuthzRolesActionsType = RefluxActions<{
  loadForUser: (username: string,
                page: number,
                perPage: number,
                query: string) => Promise<PaginatedListType>,
  loadPaginated: (page: number,
                  perPage: number,
                  query: string) => Promise<PaginatedListType>,
}>;

const AuthzRolesActions: AuthzRolesActionsType = singletonActions(
  'AuthzRoles',
  () => Reflux.createActions({
    loadForUser: { asyncResult: true },
    loadPaginated: { asyncResult: true },
  }),
);

export default AuthzRolesActions;
