// @flow strict
import Reflux from 'reflux';
import * as Immutable from 'immutable';

import { singletonActions } from 'views/logic/singleton';
import type { RefluxActions } from 'stores/StoreTypes';
import User from 'logic/users/User';
import UserOverview from 'logic/users/UserOverview';
import type { PaginationType } from 'stores/PaginationTypes';

export type Token = {
  token_name: string,
  token: string,
  last_access: string,
};

export type ChangePasswordRequest = {
  old_password: string,
  password: string,
};

export type PaginatedUsers = {
  adminUser: ?UserOverview,
  list: ?Immutable.List<UserOverview>,
  pagination: PaginationType,
};

type UsersActionsType = RefluxActions<{
  create: (request: any) => Promise<string[]>,
  loadUsers: () => Promise<Immutable.List<User>>,
  searchPaginated: (page: number, perPage: number, query: string) => Promise<PaginatedUsers>,
  load: (username: string) => Promise<User>,
  deleteUser: (username: string) => Promise<string[]>,
  changePassword: (username: string, request: ChangePasswordRequest) => Promise<void>,
  update: (username: string, request: any) => Promise<void>,
  createToken: (username: string, tokenName: string) => Promise<Token>,
  deleteToken: (username: string, tokenId: string, tokenName: string) => Promise<string[]>,
  loadTokens: (username: string) => Promise<Token[]>,
}>;

const UsersActions: UsersActionsType = singletonActions(
  'Users',
  () => Reflux.createActions({
    create: { asyncResult: true },
    loadUsers: { asyncResult: true },
    searchPaginated: { asyncResult: true },
    load: { asyncResult: true },
    deleteUser: { asyncResult: true },
    changePassword: { asyncResult: true },
    update: { asyncResult: true },
    createToken: { asyncResult: true },
    deleteToken: { asyncResult: true },
    loadTokens: { asyncResult: true },
  }),
);

export default UsersActions;
