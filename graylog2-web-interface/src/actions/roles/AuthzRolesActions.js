// @flow strict
import Reflux from 'reflux';
import * as Immutable from 'immutable';

import Role from 'logic/roles/Role';
import type { PaginatedListType, PaginatedUserListType } from 'stores/roles/AuthzRolesStore';
import { singletonActions } from 'views/logic/singleton';
import type { RefluxActions } from 'stores/StoreTypes';

export type ActionsType = {
  load: (roleId: string) => Promise<Role>,
  delete: (roleId: string, roleName: string) => Promise<void>,
  addMembers: (roleId: string, usernames: Immutable.Set<string>) => Promise<Role>,
  removeMember: (roleId: string, username: string) => Promise<Role>,
  loadUsersForRole: (roleId: string, roleName: string, page: number, perPage: number, query: string) => Promise<PaginatedUserListType>,
  loadRolesForUser: (
    username: string,
    page: number,
    perPage: number,
    query: string) => Promise<PaginatedListType>,
  loadRolesPaginated: (
    page: number,
    perPage: number,
    query: string) => Promise<PaginatedListType>,
};

const AuthzRolesActions: RefluxActions<ActionsType> = singletonActions(
  'AuthzRoles',
  () => Reflux.createActions({
    load: { asyncResult: true },
    delete: { asyncResult: true },
    addMembers: { asyncResult: true },
    removeMember: { asyncResult: true },
    loadUsersForRole: { asyncResult: true },
    loadRolesForUser: { asyncResult: true },
    loadRolesPaginated: { asyncResult: true },
  }),
);

export default AuthzRolesActions;
