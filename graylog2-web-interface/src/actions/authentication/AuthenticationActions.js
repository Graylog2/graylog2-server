// @flow strict
import Reflux from 'reflux';
import * as Immutable from 'immutable';

import { singletonActions } from 'views/logic/singleton';
import type { RefluxActions } from 'stores/StoreTypes';
import AuthenticationService from 'logic/authentication/AuthenticationService';
import AuthenticationUser from 'logic/authentication/AuthenticationUser';
import type { PaginationType } from 'stores/PaginationTypes';

export type AuthenticationServiceCreate = {
  title: string,
  description: string,
  config: {
    [string]: mixed,
  },
};

export type PaginatedServices = {
  globalConfig: {
    activeBackend: string,
  },
  list: Immutable.List<AuthenticationService>,
  pagination: PaginationType,
};

export type PaginatedAuthUsers = {
  list: Immutable.List<AuthenticationUser>,
  pagination: PaginationType,
};

export type ConnectionTestPayload = { backend_configuration: AuthenticationServiceCreate } | { backend_id: string };
export type ConnectionTestResult = {
  success: boolean,
  message: string,
  error: Array<string>,
};

export type LoginTestPayload = ConnectionTestPayload;
export type LoginTestResult = ConnectionTestResult;

export type ActionsType = {
  create: AuthenticationServiceCreate => Promise<void>,
  load: (id: string) => Promise<?AuthenticationService>,
  testConnetion: (payload: ConnectionTestPayload) => Promise<?ConnectionTestResult>,
  testLogin: (payload: LoginTestPayload) => Promise<?LoginTestResult>,
  loadServicesPaginated: (page: number, perPage: number, query: string) => Promise<?PaginatedServices>,
  loadUsersPaginated: (page: number, perPage: number, query: string) => Promise<?PaginatedAuthUsers>,
  enableUser: (userId: string) => Promise<void>,
  disableUser: (userId: string) => Promise<void>,
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
    loadServicesPaginated: { asyncResult: true },
    loadUsersPaginated: { asyncResult: true },
    enableUser: { asyncResult: true },
    disableUser: { asyncResult: true },
  }),
);

export default AuthenticationActions;
