/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import Reflux from 'reflux';
import * as Immutable from 'immutable';

import Role from 'logic/roles/Role';
import UserOverview from 'logic/users/UserOverview';
import { singletonActions } from 'views/logic/singleton';
import type { PaginatedList, Pagination } from 'stores/PaginationTypes';

export type UserContext = {
  id: string,
  username: string,
};

export type RoleContext = {
  users: { [key: string]: UserContext[] },
};

export type PaginatedRoles = PaginatedList<Role> & {
  context: RoleContext,
};
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

const AuthzRolesActions = singletonActions(
  'AuthzRoles',
  () => Reflux.createActions<ActionsType>({
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
