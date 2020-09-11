// @flow strict
import Reflux from 'reflux';
import * as Immutable from 'immutable';

import AuthenticationBackend from 'logic/authentication/AuthenticationBackend';
import { singletonActions } from 'views/logic/singleton';
import type { RefluxActions } from 'stores/StoreTypes';
import AuthenticationUser from 'logic/authentication/AuthenticationUser';
import type { PaginationType } from 'stores/PaginationTypes';

export type AuthenticationBackendCreate = {
  title: string,
  description: string,
  config: {
    [string]: mixed,
  },
};

export type PaginatedBackends = {
  globalConfig: {
    activeBackend: string,
  },
  list: Immutable.List<AuthenticationBackend>,
  pagination: PaginationType,
};

export type PaginatedAuthUsers = {
  list: Immutable.List<AuthenticationUser>,
  pagination: PaginationType,
};

export type ConnectionTestPayload = { backend_configuration: AuthenticationBackendCreate } | { backend_id: string };
export type ConnectionTestResult = {
  success: boolean,
  message: string,
  error: Array<string>,
};

export type LoginTestPayload = ConnectionTestPayload;
export type LoginTestResult = ConnectionTestResult;

export type ActionsType = {
  create: (AuthenticationBackendCreate) => Promise<void>,
  load: (id: string) => Promise<?AuthenticationBackend>,
  testConnetion: (payload: ConnectionTestPayload) => Promise<?ConnectionTestResult>,
  testLogin: (payload: LoginTestPayload) => Promise<?LoginTestResult>,
  loadBackendsPaginated: (page: number, perPage: number, query: string) => Promise<?PaginatedBackends>,
  loadUsersPaginated: (page: number, perPage: number, query: string) => Promise<?PaginatedAuthUsers>,
  enableUser: (userId: string, username: string) => Promise<void>,
  disableUser: (userId: string, username: string) => Promise<void>,
};

const AuthenticationActions: RefluxActions<ActionsType> = singletonActions(
  'Authentication',
  () => Reflux.createActions({
    legacyLoad: { asyncResult: true },
    legacyUpdate: { asyncResult: true },
    create: { asyncResult: true },
    load: { asyncResult: true },
    testConnetion: { asyncResult: true },
    testLogin: { asyncResult: true },
    loadBackendsPaginated: { asyncResult: true },
    loadUsersPaginated: { asyncResult: true },
    enableUser: { asyncResult: true },
    disableUser: { asyncResult: true },
  }),
);

export default AuthenticationActions;
