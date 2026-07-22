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
import type * as Immutable from 'immutable';
import * as ImmutableLib from 'immutable';
import { useQuery } from '@tanstack/react-query';

import type { PaginatedUsersResponse } from 'hooks/useUsers';
import fetch from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';
import { qualifyUrl } from 'util/URLUtils';
import PaginationURL from 'util/PaginationURL';
import Role from 'logic/roles/Role';
import type { RoleJSON } from 'logic/roles/Role';
import UserOverview from 'logic/users/UserOverview';
import type { PaginatedListJSON, Pagination, PaginatedList } from 'stores/PaginationTypes';

export type UserContext = {
  id: string;
  username: string;
};

export type RoleContext = {
  users: { [key: string]: UserContext[] };
};

export type PaginatedRoles = PaginatedList<Role> & {
  context: RoleContext;
};
export type PaginatedUsers = PaginatedList<UserOverview>;

export type PaginatedRolesResponse = PaginatedListJSON & {
  roles: Array<RoleJSON>;
  context?: RoleContext;
};

export const AUTHZ_ROLES_QUERY_KEY = ['authz', 'roles'] as const;

const _responseToPaginatedList = ({
  count,
  total,
  page,
  per_page,
  query,
  roles = [],
  context = { users: undefined },
}: PaginatedRolesResponse): PaginatedRoles => ({
  list: ImmutableLib.List(roles.map((r) => Role.fromJSON(r))),
  pagination: {
    query,
    page,
    perPage: per_page,
    count,
    total,
  },
  context,
});

const _responseToPaginatedUserList = ({
  count,
  total,
  page,
  per_page,
  query,
  users,
}: PaginatedUsersResponse): PaginatedUsers => ({
  list: ImmutableLib.List(users.map((u) => UserOverview.fromJSON(u))),
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

export const loadRole = (roleId: string): Promise<Role> => {
  const url = qualifyUrl(encodeApiUrl(ApiRoutes.AuthzRolesController.load, [roleId]));

  return fetch('GET', url).then(Role.fromJSON);
};

export const deleteRole = (roleId: string): Promise<void> => {
  const url = qualifyUrl(encodeApiUrl(ApiRoutes.AuthzRolesController.delete, [roleId]));

  return fetch('DELETE', url);
};

export const addMembersToRole = (roleId: string, usernames: Immutable.Set<string>): Promise<Role> => {
  const url = encodeApiUrl(ApiRoutes.AuthzRolesController.addMembers, [roleId]);

  return fetch('PUT', qualifyUrl(url), usernames.toArray());
};

export const removeMemberFromRole = (roleId: string, username: string): Promise<Role> => {
  const url = encodeApiUrl(ApiRoutes.AuthzRolesController.removeMember, [roleId, username]);

  return fetch('DELETE', qualifyUrl(url));
};

export const loadUsersForRole = (roleId: string, { page, perPage, query }: Pagination): Promise<PaginatedUsers> => {
  const apiUrl = encodeApiUrl(ApiRoutes.AuthzRolesController.loadUsersForRole, [roleId]);
  const url = PaginationURL(apiUrl, page, perPage, query);

  return fetch('GET', qualifyUrl(url)).then(_responseToPaginatedUserList);
};

export const loadRolesForUser = (username: string, { page, perPage, query }: Pagination): Promise<PaginatedRoles> => {
  const apiUrl = encodeApiUrl(ApiRoutes.AuthzRolesController.loadRolesForUser, [username]);
  const url = PaginationURL(apiUrl, page, perPage, query);

  return fetch('GET', qualifyUrl(url)).then(_responseToPaginatedList);
};

export const loadRolesPaginated = ({ page, perPage, query }: Pagination): Promise<PaginatedRoles> => {
  const apiUrl = encodeApiUrl(ApiRoutes.AuthzRolesController.list);
  const url = PaginationURL(apiUrl, page, perPage, query);

  return fetch('GET', qualifyUrl(url)).then(_responseToPaginatedList);
};

export const useAuthzRolesPaginated = (pagination: Pagination) =>
  useQuery({
    queryKey: [...AUTHZ_ROLES_QUERY_KEY, 'paginated', pagination],
    queryFn: () => loadRolesPaginated(pagination),
  });

export const useRolesForUser = (username: string, pagination: Pagination) =>
  useQuery({
    queryKey: [...AUTHZ_ROLES_QUERY_KEY, 'for-user', username, pagination],
    queryFn: () => loadRolesForUser(username, pagination),
    enabled: !!username,
  });

export const useUsersForRole = (roleId: string, pagination: Pagination) =>
  useQuery({
    queryKey: [...AUTHZ_ROLES_QUERY_KEY, 'users-for-role', roleId, pagination],
    queryFn: () => loadUsersForRole(roleId, pagination),
    enabled: !!roleId,
  });
