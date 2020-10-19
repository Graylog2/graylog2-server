// @flow strict
import Reflux from 'reflux';

import AuthenticationBackend, { type AuthenticationBackendJSON } from 'logic/authentication/AuthenticationBackend';
import { singletonActions } from 'views/logic/singleton';
import type { RefluxActions } from 'stores/StoreTypes';
import type { Pagination, PaginatedList } from 'stores/PaginationTypes';
import type { PaginatedUsers } from 'actions/users/UsersActions';

export type AuthenticationBackendCreate = {
  title: $PropertyType<AuthenticationBackendJSON, 'title'>,
  description: $PropertyType<AuthenticationBackendJSON, 'description'>,
  config: {
    type: string,
    ...any,
  },
};

export type AuthenticationBackendUpdate = {
  id: $PropertyType<AuthenticationBackendJSON, 'id'>,
  title: $PropertyType<AuthenticationBackendJSON, 'title'>,
  description: $PropertyType<AuthenticationBackendJSON, 'description'>,
  config: {
    type: string,
    ...any,
  },
};

export type PaginatedBackends = PaginatedList<AuthenticationBackend> & {
  context: {
    activeBackend: ?AuthenticationBackend,
  },
};

export type ConnectionTestPayload = {
  backend_configuration: AuthenticationBackendCreate,
  backend_id: ?string,
};
export type ConnectionTestResult = {
  success: boolean,
  message: string,
  errors: Array<string>,
};
export type LoginTestPayload = {
  backend_id: ?string,
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
  backend: ?AuthenticationBackend,
};

export type LoadActiveResponse = LoadActiveResponse & {
  context: {
    backendTotal: number,
  },
};

export type ActionsType = {
  create: (AuthenticationBackendCreate) => Promise<LoadResponse>,
  delete: (authBackendId: ?$PropertyType<AuthenticationBackend, 'id'>, authBackendTitle: $PropertyType<AuthenticationBackend, 'title'>) => Promise<void>,
  disableUser: (userId: string, username: string) => Promise<void>,
  enableUser: (userId: string, username: string) => Promise<void>,
  load: (id: string) => Promise<LoadResponse>,
  loadActive: () => Promise<LoadActiveResponse>,
  loadBackendsPaginated: (pagination: Pagination) => Promise<PaginatedBackends>,
  loadUsersPaginated: (pagination: Pagination) => Promise<PaginatedUsers>,
  setActiveBackend: (authBackendId: ?$PropertyType<AuthenticationBackend, 'id'>, authBackendTitle: $PropertyType<AuthenticationBackend, 'title'>) => Promise<void>,
  testConnection: (payload: ConnectionTestPayload) => Promise<ConnectionTestResult>,
  testLogin: (payload: LoginTestPayload) => Promise<LoginTestResult>,
  update: (id: string, AuthenticationBackendUpdate) => Promise<LoadResponse>,
};

const AuthenticationActions: RefluxActions<ActionsType> = singletonActions(
  'Authentication',
  () => Reflux.createActions({
    create: { asyncResult: true },
    delete: { asyncResult: true },
    disableUser: { asyncResult: true },
    enableUser: { asyncResult: true },
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
