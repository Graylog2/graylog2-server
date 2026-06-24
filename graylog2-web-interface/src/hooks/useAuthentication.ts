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
import { useQuery } from '@tanstack/react-query';

import AuthenticationBackend from 'logic/authentication/AuthenticationBackend';
import type { AuthenticationBackendJSON } from 'logic/authentication/AuthenticationBackend';
import { qualifyUrl } from 'util/URLUtils';
import fetch, { Builder } from 'logic/rest/FetchProvider';
import PaginationURL from 'util/PaginationURL';
import type { PaginatedUsers } from 'hooks/useUsers';
import type { PaginatedResponseType, Pagination, PaginatedList } from 'stores/PaginationTypes';
import ApiRoutes from 'routing/ApiRoutes';
import type { UserOverviewJSON } from 'logic/users/UserOverview';
import UserOverview from 'logic/users/UserOverview';

export type AuthenticationBackendCreate = {
  title: AuthenticationBackendJSON['title'];
  description: AuthenticationBackendJSON['description'];
  config: {
    type: string;
  };
};

export type AuthenticationBackendUpdate = {
  id: AuthenticationBackendJSON['id'];
  title: AuthenticationBackendJSON['title'];
  description: AuthenticationBackendJSON['description'];
  config: {
    type: string;
  };
};

export type PaginatedBackends = PaginatedList<AuthenticationBackend> & {
  context: {
    activeBackend: AuthenticationBackendJSON | undefined | null;
  };
};

export type ConnectionTestPayload = {
  backend_configuration: AuthenticationBackendCreate;
  backend_id: string | undefined | null;
};
export type ConnectionTestResult = {
  success: boolean;
  message: string;
  errors: Array<string>;
};
export type LoginTestPayload = {
  backend_id: string | undefined | null;
  backend_configuration: AuthenticationBackendCreate;
  user_login: {
    username: string;
    password: string;
  };
};

export type LoginTestResult = {
  success: boolean;
  message: string;
  errors: Array<string>;
  result: {
    user_exists: boolean;
    login_success: boolean;
    user_details: {
      dn: string;
      entryUUID: string;
      uid: string;
      cn: string;
      email: string;
    };
  };
};

export type LoadResponse = {
  backend: AuthenticationBackend | undefined | null;
};

export type LoadActiveResponse = LoadResponse & {
  context: {
    backendsTotal: number;
  };
};

type PaginatedBackendsResponse = PaginatedResponseType & {
  context: {
    active_backend: AuthenticationBackendJSON | null | undefined;
  };
  backends: Array<AuthenticationBackendJSON>;
};

type PaginatedUsersResponse = PaginatedResponseType & {
  users: Array<UserOverviewJSON>;
};

export const AUTHENTICATION_QUERY_KEY = ['authentication'] as const;

export const createAuthBackend = (authBackend: AuthenticationBackendCreate): Promise<LoadResponse> => {
  const url = qualifyUrl(ApiRoutes.AuthenticationController.create().url);

  return fetch('POST', url, authBackend).then((result) =>
    result
      ? {
          backend: AuthenticationBackend.fromJSON(result.backend),
        }
      : null,
  );
};

export const loadAuthBackend = (backendId: string): Promise<LoadResponse> => {
  const url = qualifyUrl(ApiRoutes.AuthenticationController.load(encodeURIComponent(backendId)).url);

  return fetch('GET', url).then((result) =>
    result
      ? {
          backend: AuthenticationBackend.fromJSON(result.backend),
        }
      : null,
  );
};

export const loadActiveAuthBackend = (): Promise<LoadActiveResponse> => {
  const url = qualifyUrl(ApiRoutes.AuthenticationController.loadActive().url);

  return fetch('GET', url).then((result) =>
    result
      ? {
          backend: result.backend ? AuthenticationBackend.fromJSON(result.backend) : null,
          context: { backendsTotal: result.context.backends_total },
        }
      : null,
  );
};

export const updateAuthBackend = (
  backendId: null | undefined | AuthenticationBackend['id'],
  payload: AuthenticationBackendUpdate,
): Promise<LoadResponse> => {
  const url = qualifyUrl(ApiRoutes.AuthenticationController.update(backendId).url);

  return fetch('PUT', url, payload).then((result) =>
    result
      ? {
          backend: AuthenticationBackend.fromJSON(result.backend),
        }
      : null,
  );
};

export const deleteAuthBackend = (backendId: null | undefined | AuthenticationBackend['id']): Promise<void> => {
  const url = qualifyUrl(ApiRoutes.AuthenticationController.delete(backendId).url);

  return fetch('DELETE', url);
};

export const testAuthConnection = (payload: ConnectionTestPayload): Promise<ConnectionTestResult> => {
  const url = qualifyUrl(ApiRoutes.AuthenticationController.testConnection().url);

  return fetch('POST', url, payload);
};

export const testAuthLogin = (payload: LoginTestPayload): Promise<LoginTestResult> => {
  const url = qualifyUrl(ApiRoutes.AuthenticationController.testLogin().url);

  return fetch('POST', url, payload);
};

export const setActiveAuthBackend = (backendId: null | undefined | AuthenticationBackend['id']): Promise<void> => {
  const url = qualifyUrl(ApiRoutes.AuthenticationController.updateConfiguration().url);

  return fetch('POST', url, { active_backend: backendId });
};

export const loadAuthBackendsPaginated = ({ page, perPage, query }: Pagination): Promise<PaginatedBackends> => {
  const url = PaginationURL(ApiRoutes.AuthenticationController.servicesPaginated().url, page, perPage, query);

  return fetch('GET', qualifyUrl(url)).then((response: PaginatedBackendsResponse) => ({
    context: {
      activeBackend: response.context.active_backend,
    },
    list: Immutable.List<AuthenticationBackend>(
      response.backends.map((backend) => AuthenticationBackend.fromJSON(backend)),
    ),
    pagination: {
      page: response.page,
      perPage: response.per_page,
      query: response.query,
      count: response.count,
      total: response.total,
    },
  }));
};

export const loadAuthBackendUsersPaginated = (
  authBackendId: string,
  { page, perPage, query }: Pagination,
): Promise<PaginatedUsers> => {
  const url = PaginationURL(
    ApiRoutes.AuthenticationController.loadUsersPaginated(authBackendId).url,
    page,
    perPage,
    query,
  );

  return fetch('GET', qualifyUrl(url)).then(
    (response: PaginatedUsersResponse): PaginatedUsers => ({
      list: Immutable.List<UserOverview>(response.users.map((user) => UserOverview.fromJSON(user))),
      pagination: {
        page: response.page,
        perPage: response.per_page,
        query: response.query,
        count: response.count,
        total: response.total,
      },
      adminUser: undefined,
    }),
  );
};

export const loadActiveAuthBackendType = (): Promise<string | undefined> => {
  const url = qualifyUrl(ApiRoutes.AuthenticationController.loadActiveBackendType().url);

  // no authentication required
  return new Builder('GET', url)
    .build()
    .then((response) => response.json())
    .then((result: { backend: string | undefined }) => result.backend);
};

export const useActiveAuthBackend = () =>
  useQuery({
    queryKey: [...AUTHENTICATION_QUERY_KEY, 'active'],
    queryFn: loadActiveAuthBackend,
  });
