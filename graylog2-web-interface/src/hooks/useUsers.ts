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
import * as Immutable from 'immutable';
import URI from 'urijs';
import { keepPreviousData, useQuery } from '@tanstack/react-query';

import type { UserOverviewJSON, AccountStatus } from 'logic/users/UserOverview';
import UserOverview from 'logic/users/UserOverview';
import fetch from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';
import { qualifyUrl } from 'util/URLUtils';
import PaginationURL from 'util/PaginationURL';
import type { UserJSON } from 'logic/users/User';
import User from 'logic/users/User';
import type { PaginatedListJSON, Pagination, PaginatedList } from 'stores/PaginationTypes';

export type PaginatedUsersResponse = PaginatedListJSON & {
  users: Array<UserOverviewJSON>;
  context: {
    admin_user: UserOverviewJSON;
  };
};

export type UserCreate = {
  email: UserJSON['email'];
  full_name: UserJSON['full_name'];
  first_name: UserJSON['first_name'];
  last_name: UserJSON['last_name'];
  password: string;
  permissions: UserJSON['permissions'];
  roles: UserJSON['roles'];
  session_timeout_ms: UserJSON['session_timeout_ms'];
  timezone: UserJSON['timezone'];
  username: UserJSON['username'];
};

export type UserUpdate = Partial<
  UserCreate & {
    old_password: string;
  }
>;

export type Token = {
  id: string;
  name: string;
  token: string;
  last_access: string;
  expires_at: string;
};

export type TokenSummary = {
  id: string;
  name: string;
  last_access: string;
  created_at: string;
  expires_at: string;
  tokenTtl: string;
};

export type ChangePasswordRequest = {
  old_password: string;
  password: string;
};

export type PaginatedUsers = PaginatedList<UserOverview> & {
  adminUser: UserOverview | null | undefined;
};

export type Query = {
  include_permissions?: boolean;
  include_sessions?: boolean;
};

export const USERS_QUERY_KEY = ['users'] as const;

const usersUrl = ({ url = '', query = {} }) => {
  const uri = new URI(url);

  uri.query(query);

  return qualifyUrl(uri.resource());
};

export const createUser = (user: UserCreate): Promise<void> => {
  const url = qualifyUrl(ApiRoutes.UsersApiController.create().url);

  return fetch('POST', url, user);
};

export const loadUser = (id: string): Promise<User> => {
  const url = qualifyUrl(ApiRoutes.UsersApiController.load(encodeURIComponent(id)).url);

  return fetch('GET', url).then(User.fromJSON);
};

export const loadUserByUsername = (username: string): Promise<User> => {
  const url = qualifyUrl(ApiRoutes.UsersApiController.loadByUsername(encodeURIComponent(username)).url);

  return fetch('GET', url).then(User.fromJSON);
};

export const updateUser = (userId: string, request: UserUpdate): Promise<void> => {
  const url = qualifyUrl(ApiRoutes.UsersApiController.update(encodeURIComponent(userId)).url);

  return fetch('PUT', url, request);
};

export const deleteUser = (userId: string): Promise<void> => {
  const url = qualifyUrl(ApiRoutes.UsersApiController.delete(encodeURIComponent(userId)).url);

  return fetch('DELETE', url);
};

export const changeUserPassword = (userId: string, request: ChangePasswordRequest): Promise<void> => {
  const url = qualifyUrl(ApiRoutes.UsersApiController.changePassword(encodeURIComponent(userId)).url);

  return fetch('PUT', url, request);
};

export const createUserToken = (userId: string, tokenName: string, tokenTtl: string): Promise<Token> => {
  const url = qualifyUrl(
    ApiRoutes.UsersApiController.create_token(encodeURIComponent(userId), encodeURIComponent(tokenName)).url,
  );

  return fetch('POST', url, { token_ttl: tokenTtl });
};

export const loadUserTokens = (userId: string): Promise<TokenSummary[]> => {
  const url = qualifyUrl(ApiRoutes.UsersApiController.list_tokens(encodeURIComponent(userId)).url);

  return fetch('GET', url).then((response) => response.tokens);
};

export const deleteUserToken = (userId: string, tokenId: string): Promise<void> => {
  const url = qualifyUrl(
    ApiRoutes.UsersApiController.delete_token(encodeURIComponent(userId), encodeURIComponent(tokenId)).url,
  );

  return fetch('DELETE', url);
};

export const loadUsers = (query: Query = {}): Promise<Immutable.List<User>> => {
  const url = usersUrl({ url: ApiRoutes.UsersApiController.list().url, query });

  return fetch('GET', url).then(({ users }: { users: Array<UserOverviewJSON> }) =>
    Immutable.List<User>(users.map((user) => UserOverview.fromJSON(user))),
  );
};

export const loadUsersPaginated = ({ page, perPage, query }: Pagination): Promise<PaginatedUsers> => {
  const url = PaginationURL(ApiRoutes.UsersApiController.paginated().url, page, perPage, query);

  return fetch('GET', qualifyUrl(url)).then((response: PaginatedUsersResponse) => ({
    adminUser: response.context?.admin_user ? UserOverview.fromJSON(response.context.admin_user) : undefined,
    list: Immutable.List(response.users.map((user) => UserOverview.fromJSON(user))),
    pagination: {
      page: response.page,
      perPage: response.per_page,
      query: response.query,
      count: response.count,
      total: response.total,
    },
  }));
};

export const setUserStatus = (userId: string, accountStatus: AccountStatus): Promise<void> => {
  const url = qualifyUrl(ApiRoutes.UsersApiController.setStatus(userId, accountStatus).url);

  return fetch('PUT', url);
};

export const useUsers = (query: Query = {}) =>
  useQuery({
    queryKey: [...USERS_QUERY_KEY, 'overview', query],
    queryFn: () => loadUsers(query),
  });

export const useUsersPaginated = (pagination: Pagination) =>
  useQuery({
    queryKey: [...USERS_QUERY_KEY, 'paginated', pagination],
    queryFn: () => loadUsersPaginated(pagination),
    placeholderData: keepPreviousData,
  });
