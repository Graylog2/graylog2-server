// @flow strict
import Reflux from 'reflux';
import * as Immutable from 'immutable';

import { singletonActions } from 'views/logic/singleton';
import type { RefluxActions } from 'stores/StoreTypes';
import AuthenticationService from 'logic/authentication/AuthenticationService';
import type { PaginationType } from 'stores/PaginationTypes';

export type PaginatedServices = {
  globalConfig: {
    activeBackend: string,
  },
  list: Immutable.List<AuthenticationService>,
  pagination: PaginationType,
};

export type ActionsType = {
  loadServicesPaginated: (page: number, perPage: number, query: string) => Promise<?PaginatedServices>,
};

const AuthenticationActions: RefluxActions<ActionsType> = singletonActions(
  'Authentication',
  () => Reflux.createActions({
    load: { asyncResult: true },
    loadServicesPaginated: { asyncResult: true },
    update: { asyncResult: true },
  }),
);

export default AuthenticationActions;
