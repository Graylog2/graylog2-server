// @flow strict
import Reflux from 'reflux';

import Role from 'logic/roles/Role';
import type { PaginatedListType, PaginatedUserListType } from 'stores/roles/AuthzRolesStore';
import { singletonActions } from 'views/logic/singleton';
import type { RefluxActions } from 'stores/StoreTypes';

type AuthzRolesActionsType = RefluxActions<{
  deleteRole: (roleId: string) => Promise<string[]>,
  addMember: (roleId: string, username: string) => Promise<Role>,
  removeMember: (roleId: string, username: string) => Promise<Role>,
  loadUsersForRole: (roleId: string, page: number, perPage: number, query: string) => Promise<PaginatedUserListType>,
  load: (roleId: string) => Promise<Role>,
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
    addMember: { asyncResult: true },
    removeMember: { asyncResult: true },
    loadUsersForRole: { asyncResult: true },
    deleteRole: { asyncResult: true },
    load: { asyncResult: true },
    loadForUser: { asyncResult: true },
    loadPaginated: { asyncResult: true },
  }),
);

export default AuthzRolesActions;
