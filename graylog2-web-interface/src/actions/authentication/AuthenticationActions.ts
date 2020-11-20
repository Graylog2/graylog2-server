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
import { $PropertyType } from 'utility-types';

import AuthenticationBackend, { AuthenticationBackendJSON } from 'logic/authentication/AuthenticationBackend';
import { singletonActions } from 'views/logic/singleton';
import type { RefluxActions } from 'stores/StoreTypes';
import type { Pagination, PaginatedList } from 'stores/PaginationTypes';
import type { PaginatedUsers } from 'actions/users/UsersActions';

export type AuthenticationBackendCreate = {
  title: $PropertyType<AuthenticationBackendJSON, 'title'>,
  description: $PropertyType<AuthenticationBackendJSON, 'description'>,
  config: {
    type: string,
  },
};

export type AuthenticationBackendUpdate = {
  id: $PropertyType<AuthenticationBackendJSON, 'id'>,
  title: $PropertyType<AuthenticationBackendJSON, 'title'>,
  description: $PropertyType<AuthenticationBackendJSON, 'description'>,
  config: {
    type: string,
  },
};

export type PaginatedBackends = PaginatedList<AuthenticationBackend> & {
  context: {
    activeBackend: AuthenticationBackend | undefined | null,
  },
};

export type ConnectionTestPayload = {
  backend_configuration: AuthenticationBackendCreate,
  backend_id: string | undefined | null,
};
export type ConnectionTestResult = {
  success: boolean,
  message: string,
  errors: Array<string>,
};
export type LoginTestPayload = {
  backend_id: string | undefined | null,
  backend_configuration: AuthenticationBackendCreate,
  user_login: {
    username: string,
    password: string,
  },
};

export type LoginTestResult = {
  success: boolean,
  message: string,
  errors: Array<string>,
  result: {
    user_exists: boolean,
    login_success: boolean,
    user_details: {
      dn: string,
      entryUUID: string,
      uid: string,
      cn: string,
      email: string,
    },
  },
};

export type LoadResponse = {
  backend: AuthenticationBackend | undefined | null,
};

export type LoadActiveResponse = LoadResponse & {
  context: {
    backendsTotal: number,
  },
};

export type ActionsType = {
  create: (AuthenticationBackendCreate) => Promise<LoadResponse>,
  delete: (authBackendId: $PropertyType<AuthenticationBackend | undefined | null, 'id'>, authBackendTitle: $PropertyType<AuthenticationBackend, 'title'>) => Promise<void>,
  load: (id: string) => Promise<LoadResponse>,
  loadActive: () => Promise<LoadActiveResponse>,
  loadBackendsPaginated: (pagination: Pagination) => Promise<PaginatedBackends>,
  loadUsersPaginated: (authBackendId: string, pagination: Pagination) => Promise<PaginatedUsers>,
  setActiveBackend: (authBackendId: $PropertyType<AuthenticationBackend, 'id'> | undefined | null, authBackendTitle: $PropertyType<AuthenticationBackend, 'title'>) => Promise<void>,
  testConnection: (payload: ConnectionTestPayload) => Promise<ConnectionTestResult>,
  testLogin: (payload: LoginTestPayload) => Promise<LoginTestResult>,
  update: (id: string, AuthenticationBackendUpdate) => Promise<LoadResponse>,
};

const AuthenticationActions: RefluxActions<ActionsType> = singletonActions(
  'Authentication',
  () => Reflux.createActions({
    create: { asyncResult: true },
    delete: { asyncResult: true },
    load: { asyncResult: true },
    loadActive: { asyncResult: true },
    loadBackendsPaginated: { asyncResult: true },
    loadUsersPaginated: { asyncResult: true },
    setActiveBackend: { asyncResult: true },
    testConnection: { asyncResult: true },
    testLogin: { asyncResult: true },
    update: { asyncResult: true },
  }),
);

export default AuthenticationActions;
