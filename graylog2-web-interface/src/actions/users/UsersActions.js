// @flow strict
import Reflux from 'reflux';

import { singletonActions } from 'views/logic/singleton';
import type { User, ChangePasswordRequest, Token } from 'stores/users/UsersStore';

type UsersActionsType = {
  create: (request: any) => Promise<string[]>,
  loadUsers: () => Promise<User[]>,
  load: (username: string) => Promise<User>,
  deleteUser: (username: string) => Promise<string[]>,
  changePassword: (username: string, request: ChangePasswordRequest) => void,
  update: (username: string, request: any) => void,
  createToken: (username: string, tokenName: string) => Promise<Token>,
  deleteToken: (username: string, tokenId: string, tokenName: string) => Promise<string[]>,
  loadTokens: (username: string) => Promise<Token[]>,
};

const UsersActions: UsersActionsType = singletonActions(
  'Users',
  () => Reflux.createActions({
    create: { asyncResult: true },
    loadUsers: { asyncResult: true },
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
