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
// @flow strict
import Reflux from 'reflux';
import * as Immutable from 'immutable';

import type { PaginatedUsersResponse } from 'stores/users/UsersStore';
import type { Store } from 'stores/StoreTypes';
import fetch from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';
import { qualifyUrl } from 'util/URLUtils';
import { singletonStore } from 'views/logic/singleton';
import PaginationURL from 'util/PaginationURL';
import Role from 'logic/roles/Role';
import type { RoleJSON } from 'logic/roles/Role';
import AuthzRolesActions, { type PaginatedRoles, type PaginatedUsers, type RoleContext } from 'actions/roles/AuthzRolesActions';
import UserOverview from 'logic/users/UserOverview';
import type { PaginatedListJSON, Pagination } from 'stores/PaginationTypes';

export type PaginatedRolesResponse = PaginatedListJSON & {
  roles: Array<RoleJSON>,
  context?: RoleContext,
};

// eslint-disable-next-line camelcase
const _responseToPaginatedList = ({ count, total, page, per_page, query, roles = [], context = {} }: PaginatedRolesResponse) => ({
  list: Immutable.List(roles.map((r) => Role.fromJSON(r))),
  pagination: {
    query,
    page,
    perPage: per_page,
    count,
    total,
  },
  context,
});

// eslint-disable-next-line camelcase
const _responseToPaginatedUserList = ({ count, total, page, per_page, query, users }: PaginatedUsersResponse) => ({
  list: Immutable.List(users.map((u) => UserOverview.fromJSON(u))),
  pagination: {
    page,
    perPage: per_page,
    query,
    count,
    total,
  },
});

const AuthzRolesStore: Store<{}> = singletonStore(
  'AuthzRoles',
  () => Reflux.createStore({
    listenables: [AuthzRolesActions],

    load(roleId: $PropertyType<Role, 'id'>): Promise<Role> {
      const url = qualifyUrl(ApiRoutes.AuthzRolesController.load(encodeURIComponent(roleId)).url);
      const promise = fetch('GET', url).then(Role.fromJSON);

      AuthzRolesActions.load.promise(promise);

      return promise;
    },

    delete(roleId: string): Promise<void> {
      const url = qualifyUrl(ApiRoutes.AuthzRolesController.delete(encodeURIComponent(roleId)).url);
      const promise = fetch('DELETE', url);

      AuthzRolesActions.delete.promise(promise);

      return promise;
    },

    addMembers(roleId: string, usernames: Immutable.Set<string>): Promise<Role> {
      const { url } = ApiRoutes.AuthzRolesController.addMembers(roleId);
      const promise = fetch('PUT', qualifyUrl(url), usernames.toArray());

      AuthzRolesActions.addMembers.promise(promise);

      return promise;
    },

    removeMember(roleId: string, username: string): Promise<Role> {
      const { url } = ApiRoutes.AuthzRolesController.removeMember(roleId, username);
      const promise = fetch('DELETE', qualifyUrl(url));

      AuthzRolesActions.removeMember.promise(promise);

      return promise;
    },

    loadUsersForRole(roleId: string, roleName: string, { page, perPage, query }: Pagination): Promise<PaginatedUsers> {
      const url = PaginationURL(ApiRoutes.AuthzRolesController.loadUsersForRole(roleId).url, page, perPage, query);

      const promise = fetch('GET', qualifyUrl(url))
        .then(_responseToPaginatedUserList);

      AuthzRolesActions.loadUsersForRole.promise(promise);

      return promise;
    },

    loadRolesForUser(username: string, { page, perPage, query }: Pagination): Promise<PaginatedRoles> {
      const url = PaginationURL(ApiRoutes.AuthzRolesController.loadRolesForUser(username).url, page, perPage, query);

      const promise = fetch('GET', qualifyUrl(url))
        .then(_responseToPaginatedList);

      AuthzRolesActions.loadRolesForUser.promise(promise);

      return promise;
    },

    loadRolesPaginated({ page, perPage, query }: Pagination): Promise<PaginatedRoles> {
      const url = PaginationURL(ApiRoutes.AuthzRolesController.list().url, page, perPage, query);

      const promise = fetch('GET', qualifyUrl(url))
        .then(_responseToPaginatedList);

      AuthzRolesActions.loadRolesPaginated.promise(promise);

      return promise;
    },
  }),
);

export { AuthzRolesActions, AuthzRolesStore };
