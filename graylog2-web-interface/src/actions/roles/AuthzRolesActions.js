// @flow strict
import Reflux from 'reflux';

import Role from 'logic/roles/Role';
import type { PaginatedListType, PaginatedUserListType } from 'stores/roles/AuthzRolesStore';
import { singletonActions } from 'views/logic/singleton';
import type { RefluxActions } from 'stores/StoreTypes';

type AuthzRolesActionsType = RefluxActions<{
  load: (roleId: string) => Promise<Role>,
  delete: (roleId: string) => Promise<void>,
  addMember: (roleId: string, username: string) => Promise<Role>,
  removeMember: (roleId: string, username: string) => Promise<Role>,
  loadUsersForRole: (roleId: string, page: number, perPage: number, query: string) => Promise<PaginatedUserListType>,
  loadRolesForUser: (
    username: string,
    page: number,
    perPage: number,
    query: string) => Promise<PaginatedListType>,
  loadRolesPaginated: (
    page: number,
    perPage: number,
    query: string) => Promise<PaginatedListType>,
}>;

const AuthzRolesActions: AuthzRolesActionsType = singletonActions(
  'AuthzRoles',
  () => Reflux.createActions({
    load: { asyncResult: true },
    delete: { asyncResult: true },
    addMember: { asyncResult: true },
    removeMember: { asyncResult: true },
    loadUsersForRole: { asyncResult: true },
    loadRolesForUser: { asyncResult: true },
    loadRolesPaginated: { asyncResult: true },
  }),
);

export default AuthzRolesActions;
