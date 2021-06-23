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
import { $PropertyType } from 'utility-types';

import type { PaginatedUsersResponse } from 'stores/users/UsersStore';
import type { Store } from 'stores/StoreTypes';
import fetch from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';
import { qualifyUrl } from 'util/URLUtils';
import { singletonStore } from 'views/logic/singleton';
import PaginationURL from 'util/PaginationURL';
import Role from 'logic/roles/Role';
import type { RoleJSON } from 'logic/roles/Role';
import AuthzRolesActions, { PaginatedRoles, PaginatedUsers, RoleContext } from 'actions/roles/AuthzRolesActions';
import UserOverview from 'logic/users/UserOverview';
import type { PaginatedListJSON, Pagination } from 'stores/PaginationTypes';

export type PaginatedRolesResponse = PaginatedListJSON & {
  roles: Array<RoleJSON>,
  context?: RoleContext,
};

const _responseToPaginatedList = ({ count, total, page, per_page, query, roles = [], context = { users: undefined } }: PaginatedRolesResponse) => ({
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

const encodeApiUrl = (apiRoute: (...args: Array<string>) => { url: string }, uriParams: Array<string> = []): string => {
  const encodedUriParams = uriParams.map((param) => encodeURIComponent(param));

  return apiRoute(...encodedUriParams).url;
};

const AuthzRolesStore: Store<{}> = singletonStore(
  'AuthzRoles',
  () => Reflux.createStore({
    listenables: [AuthzRolesActions],

    load(roleId: $PropertyType<Role, 'id'>): Promise<Role> {
      const url = qualifyUrl(encodeApiUrl(ApiRoutes.AuthzRolesController.load, [roleId]));
      const promise = fetch('GET', url).then(Role.fromJSON);

      AuthzRolesActions.load.promise(promise);

      return promise;
    },

    delete(roleId: string): Promise<void> {
      const url = qualifyUrl(encodeApiUrl(ApiRoutes.AuthzRolesController.delete, [roleId]));
      const promise = fetch('DELETE', url);

      AuthzRolesActions.delete.promise(promise);

      return promise;
    },

    addMembers(roleId: string, usernames: Immutable.Set<string>): Promise<Role> {
      const url = encodeApiUrl(ApiRoutes.AuthzRolesController.addMembers, [roleId]);
      const promise = fetch('PUT', qualifyUrl(url), usernames.toArray());

      AuthzRolesActions.addMembers.promise(promise);

      return promise;
    },

    removeMember(roleId: string, username: string): Promise<Role> {
      const url = encodeApiUrl(ApiRoutes.AuthzRolesController.removeMember, [roleId, username]);
      const promise = fetch('DELETE', qualifyUrl(url));

      AuthzRolesActions.removeMember.promise(promise);

      return promise;
    },

    loadUsersForRole(roleId: string, roleName: string, { page, perPage, query }: Pagination): Promise<PaginatedUsers> {
      const apiUrl = encodeApiUrl(ApiRoutes.AuthzRolesController.loadUsersForRole, [roleId]);
      const url = PaginationURL(apiUrl, page, perPage, query);

      const promise = fetch('GET', qualifyUrl(url))
        .then(_responseToPaginatedUserList);

      AuthzRolesActions.loadUsersForRole.promise(promise);

      return promise;
    },

    loadRolesForUser(username: string, { page, perPage, query }: Pagination): Promise<PaginatedRoles> {
      const apiUrl = encodeApiUrl(ApiRoutes.AuthzRolesController.loadRolesForUser, [username]);
      const url = PaginationURL(apiUrl, page, perPage, query);

      const promise = fetch('GET', qualifyUrl(url))
        .then(_responseToPaginatedList);

      AuthzRolesActions.loadRolesForUser.promise(promise);

      return promise;
    },

    loadRolesPaginated({ page, perPage, query }: Pagination): Promise<PaginatedRoles> {
      const apiUrl = encodeApiUrl(ApiRoutes.AuthzRolesController.list);
      const url = PaginationURL(apiUrl, page, perPage, query);

      const promise = fetch('GET', qualifyUrl(url))
        .then(_responseToPaginatedList);

      AuthzRolesActions.loadRolesPaginated.promise(promise);

      return promise;
    },
  }),
);

export { AuthzRolesActions, AuthzRolesStore };
