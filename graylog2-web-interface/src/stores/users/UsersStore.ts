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
import Reflux from 'reflux';
import * as Immutable from 'immutable';

import { Store } from 'stores/StoreTypes';
import UserOverview, { UserOverviewJSON, AccountStatus } from 'logic/users/UserOverview';
import fetch from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';
import { singletonStore } from 'views/logic/singleton';
import { qualifyUrl } from 'util/URLUtils';
import PaginationURL from 'util/PaginationURL';
import User from 'logic/users/User';
import UsersActions, { ChangePasswordRequest, Token, PaginatedUsers, UserCreate, UserUpdate } from 'actions/users/UsersActions';
import { PaginatedListJSON, Pagination } from 'stores/PaginationTypes';

export type PaginatedUsersResponse = PaginatedListJSON & {
  users: Array<UserOverviewJSON>;
  context: {
    admin_user: UserOverviewJSON;
  };
};

const UsersStore: Store<undefined> = singletonStore('Users', () => Reflux.createStore({
  listenables: [UsersActions],

  create(user: UserCreate): Promise<void> {
    const url = qualifyUrl(ApiRoutes.UsersApiController.create().url);
    const promise = fetch('POST', url, user);
    UsersActions.create.promise(promise);

    return promise;
  },

  load(id: string): Promise<User> {
    const url = qualifyUrl(ApiRoutes.UsersApiController.load(encodeURIComponent(id)).url);
    const promise = fetch('GET', url).then(User.fromJSON);

    UsersActions.load.promise(promise);

    return promise;
  },

  loadByUsername(userId: string): Promise<User> {
    const url = qualifyUrl(ApiRoutes.UsersApiController.loadByUsername(encodeURIComponent(userId)).url);
    const promise = fetch('GET', url).then(User.fromJSON);

    UsersActions.loadByUsername.promise(promise);

    return promise;
  },

  update(userId: string, user: UserUpdate): Promise<void> {
    const url = qualifyUrl(ApiRoutes.UsersApiController.update(encodeURIComponent(userId)).url);
    const promise = fetch('PUT', url, user);
    UsersActions.update.promise(promise);

    return promise;
  },

  delete(userId: string): Promise<void> {
    const url = qualifyUrl(ApiRoutes.UsersApiController.delete(encodeURIComponent(userId)).url);
    const promise = fetch('DELETE', url);

    UsersActions.delete.promise(promise);

    return promise;
  },

  changePassword(userId: string, request: ChangePasswordRequest): Promise<void> {
    const url = qualifyUrl(ApiRoutes.UsersApiController.changePassword(encodeURIComponent(userId)).url);
    const promise = fetch('PUT', url, request);
    UsersActions.changePassword.promise(promise);

    return promise;
  },

  createToken(userId: string, tokenName: string): Promise<Token> {
    const url = qualifyUrl(ApiRoutes.UsersApiController.create_token(encodeURIComponent(userId), encodeURIComponent(tokenName)).url);
    const promise = fetch('POST', url);
    UsersActions.createToken.promise(promise);

    return promise;
  },

  loadTokens(userId: string): Promise<Token[]> {
    const url = qualifyUrl(ApiRoutes.UsersApiController.list_tokens(encodeURIComponent(userId)).url);
    const promise = fetch('GET', url).then((response) => response.tokens);
    UsersActions.loadTokens.promise(promise);

    return promise;
  },

  deleteToken(userId: string, tokenId: string): Promise<string[]> {
    const url = qualifyUrl(ApiRoutes.UsersApiController.delete_token(encodeURIComponent(userId), encodeURIComponent(tokenId)).url, {});
    const promise = fetch('DELETE', url);
    UsersActions.deleteToken.promise(promise);

    return promise;
  },

  loadUsers(): Promise<Immutable.List<User>> {
    const url = qualifyUrl(ApiRoutes.UsersApiController.list().url);
    const promise = fetch('GET', url).then(({
      users,
    }) => Immutable.List(users.map((user) => UserOverview.fromJSON(user))));
    UsersActions.loadUsers.promise(promise);

    return promise;
  },

  loadUsersPaginated({
    page,
    perPage,
    query,
  }: Pagination): Promise<PaginatedUsers> {
    const url = PaginationURL(ApiRoutes.UsersApiController.paginated().url, page, perPage, query);

    const promise = fetch('GET', qualifyUrl(url)).then((response: PaginatedUsersResponse) => ({
      adminUser: response.context.admin_user ? UserOverview.fromJSON(response.context.admin_user) : undefined,
      list: Immutable.List(response.users.map((user) => UserOverview.fromJSON(user))),
      pagination: {
        page: response.page,
        perPage: response.per_page,
        query: response.query,
        count: response.count,
        total: response.total,
      },
    }));

    UsersActions.loadUsersPaginated.promise(promise);

    return promise;
  },

  setStatus(userId: string, accountStatus: AccountStatus): Promise<void> {
    const url = qualifyUrl(ApiRoutes.UsersApiController.setStatus(userId, accountStatus).url);
    const promise = fetch('PUT', url);
    UsersActions.setStatus.promise(promise);

    return promise;
  },
}));

export { UsersActions, UsersStore };
export default UsersStore;
