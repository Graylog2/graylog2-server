// @flow strict
import Reflux from 'reflux';
import * as Immutable from 'immutable';

import type { Store } from 'stores/StoreTypes';
import fetch from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';
import { qualifyUrl } from 'util/URLUtils';
import { singletonStore } from 'views/logic/singleton';
import PaginationURL from 'util/PaginationURL';
import Role from 'logic/roles/Role';
import type { RoleJSON } from 'logic/roles/Role';
import AuthzRolesActions from 'actions/roles/AuthzRolesActions';

import type { PaginatedResponseType, PaginationType } from '../PaginationTypes';

type PaginatedResponse = PaginatedResponseType & {
  roles: Array<RoleJSON>,
};

export type PaginatedListType = {
  list: Immutable.List<Role>,
  pagination: PaginationType,
};

// eslint-disable-next-line camelcase
const _responseToPaginatedList = ({ count, total, page, per_page, query, roles = [] }: PaginatedResponse) => {
  return {
    list: Immutable.List(roles.map((r) => Role.fromJSON(r))),
    pagination: {
      count,
      total,
      page,
      perPage: per_page,
      query,
    },
  };
};

const AuthzRolesStore: Store<{}> = singletonStore(
  'AuthzRoles',
  () => Reflux.createStore({
    listenables: [AuthzRolesActions],

    deleteUser(roleId: string): Promise<string[]> {
      const url = qualifyUrl(ApiRoutes.AuthzRolesController.delete(encodeURIComponent(roleId)).url);
      const promise = fetch('DELETE', url);

      AuthzRolesActions.deleteRole.promise(promise);

      return promise;
    },

    loadForUser(username: string, page: number, perPage: number, query: string): Promise<PaginatedListType> {
      const url = PaginationURL(ApiRoutes.AuthzRolesController.loadForUser(username).url, page, perPage, query);

      const promise = fetch('GET', qualifyUrl(url))
        .then(_responseToPaginatedList);

      AuthzRolesActions.loadForUser.promise(promise);

      return promise;
    },

    loadPaginated(page: number, perPage: number, query: string): Promise<PaginatedListType> {
      const url = PaginationURL(ApiRoutes.AuthzRolesController.load().url, page, perPage, query);

      const promise = fetch('GET', qualifyUrl(url))
        .then(_responseToPaginatedList);

      AuthzRolesActions.loadPaginated.promise(promise);

      return promise;
    },
  }),
);

export { AuthzRolesActions, AuthzRolesStore };
