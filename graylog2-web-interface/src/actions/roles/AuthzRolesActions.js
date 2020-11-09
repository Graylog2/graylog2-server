// @flow strict
import Reflux from 'reflux';
import * as Immutable from 'immutable';

import Role from 'logic/roles/Role';
import UserOverview from 'logic/users/UserOverview';
import { singletonActions } from 'views/logic/singleton';
import type { RefluxActions } from 'stores/StoreTypes';
import type { PaginatedList, Pagination } from 'stores/PaginationTypes';

export type PaginatedRoles = PaginatedList<Role>;
export type PaginatedUsers = PaginatedList<UserOverview>;

export type ActionsType = {
  load: (roleId: string) => Promise<Role>,
  delete: (roleId: string, roleName: string) => Promise<void>,
  addMembers: (roleId: string, usernames: Immutable.Set<string>) => Promise<Role>,
  removeMember: (roleId: string, username: string) => Promise<Role>,
  loadUsersForRole: (roleId: string, roleName: string, pagination: Pagination) => Promise<PaginatedUsers>,
  loadRolesForUser: (username: string, pagination: Pagination) => Promise<PaginatedRoles>,
  loadRolesPaginated: (pagination: Pagination) => Promise<PaginatedRoles>,
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
