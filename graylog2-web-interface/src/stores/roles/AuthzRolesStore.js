// @flow strict

import Reflux from 'reflux';
import * as Immutable from 'immutable';

import type { Store } from 'stores/StoreTypes';
import fetch from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import { singletonStore } from 'views/logic/singleton';
import PaginationURL from 'util/PaginationURL';
import Role from 'logic/roles/Role';
import type { RoleJSON } from 'logic/roles/Role';
import AuthzRolesActions from 'actions/roles/AuthzRolesActions';

import type { PaginatedResponseType, PaginationType } from '../PaginationTypes';

type PaginatedResponse = PaginatedResponseType & {
  roles: Array<RoleJSON>,
};

export type PaginatedList = {
  list: Immutable.List<Role>,
  pagination: PaginationType,
};

type AuthzRolesStoreState = {};

type AuthzRolesStoreType = Store<AuthzRolesStoreState>;

// eslint-disable-next-line camelcase
const _responseToPaginatedList = ({ count, total, page, per_page, query, roles }: PaginatedResponse) => {
  return {
    list: Immutable.List.of(roles.map((r) => Role.fromJSON(r))),
    pagination: {
      count,
      total,
      page,
      perPage: per_page,
      query,
    },
  };
};

const AuthzRolesStore: AuthzRolesStoreType = singletonStore(
  'AuthzRoles',
  () => Reflux.createStore({
    listenables: [AuthzRolesActions],

    loadForUser(username: string, page: number, perPage: number, query: string): Promise<PaginatedList> {
      const url = PaginationURL(ApiRoutes.AuthzRolesController.loadForUser(username).url, page, perPage, query);

      console.log("store", username);
      const promise = fetch('GET', qualifyUrl(url))
        .then(_responseToPaginatedList,
          (error) => {
            if (error.additional.status !== 404) {
              UserNotification.error(`Loading roles for user ${username} failed with status: ${error}`,
                'Could not load roles for user');
            }
          });

      AuthzRolesActions.loadForUser.promise(promise);

      return promise;
    },
  }),
);

export default AuthzRolesStore;
