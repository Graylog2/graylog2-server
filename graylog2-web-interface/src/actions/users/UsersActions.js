// @flow strict
import Reflux from 'reflux';
import * as Immutable from 'immutable';

import { singletonActions } from 'views/logic/singleton';
import type { RefluxActions } from 'stores/StoreTypes';
import User, { type UserJSON } from 'logic/users/User';
import UserOverview from 'logic/users/UserOverview';
import type { PaginationType } from 'stores/PaginationTypes';

export type UserCreate = {
  email: $PropertyType<UserJSON, 'email'>,
  full_name: $PropertyType<UserJSON, 'full_name'>,
  password: string,
  permissions: $PropertyType<UserJSON, 'permissions'>,
  roles: $PropertyType<UserJSON, 'roles'>,
  session_timeout_ms: $PropertyType<UserJSON, 'session_timeout_ms'>,
  timezone: $PropertyType<UserJSON, 'timezone'>,
  username: $PropertyType<UserJSON, 'username'>,
};

export type UserUpdate = $Shape<UserCreate & {
  old_password: string,
}>;

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
  list: Immutable.List<UserOverview>,
  pagination: PaginationType,
};

export type ActionsType = {
  create: (payload: any) => Promise<?string[]>,
  load: (username: string) => Promise<?User>,
  update: (username: string, request: any) => Promise<void>,
  delete: (username: string) => Promise<void>,
  changePassword: (username: string, request: ChangePasswordRequest) => Promise<void>,
  createToken: (username: string, tokenName: string) => Promise<?Token>,
  loadTokens: (username: string) => Promise<?Token[]>,
  deleteToken: (username: string, tokenId: string, tokenName: string) => Promise<void>,
  loadUsers: () => Promise<?Immutable.List<User>>,
  loadUsersPaginated: (page: number, perPage: number, query: string) => Promise<?PaginatedUsers>,
};

const UsersActions: RefluxActions<ActionsType> = singletonActions(
  'Users',
  () => Reflux.createActions({
    create: { asyncResult: true },
    load: { asyncResult: true },
    update: { asyncResult: true },
    delete: { asyncResult: true },
    changePassword: { asyncResult: true },
    createToken: { asyncResult: true },
    loadTokens: { asyncResult: true },
    deleteToken: { asyncResult: true },
    loadUsersPaginated: { asyncResult: true },
    loadUsers: { asyncResult: true },
  }),
);

export default UsersActions;
