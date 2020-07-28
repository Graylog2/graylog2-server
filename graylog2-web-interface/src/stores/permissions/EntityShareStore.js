// @flow strict
import Reflux from 'reflux';
import * as Immutable from 'immutable';

import ApiRoutes from 'routing/ApiRoutes';
import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import type { RefluxActions, Store } from 'stores/StoreTypes';
import { singletonActions, singletonStore } from 'views/logic/singleton';
import type { GRN, ShareEntity as ShareEntityType } from 'logic/permissions/types';
// import PaginationURL from 'util/PaginationURL';
import EntityShareState, { type EntityShareStateJson, type SelectedGranteeCapabilities } from 'logic/permissions/EntityShareState';
import ShareEntity from 'logic/permissions/ShareEntity';

type EntityShareStoreState = {
  state: EntityShareState,
};

// type PaginatedUserSharesResponse = {
//   total: number,
//   count: number,
//   page: number,
//   per_page: number,
//   query: string,
//   entities: Array<ShareEntityType>,
//   context: {
//     user_capabilities: { [grn: GRN]: string },
//   },
// };

type PaginatedUserSharesType = {
  list: Immutable.List<ShareEntity>,
  context: {
    userCapabilities: { [grn: GRN]: string },
  },
};

export type EntitySharePayload = {
  selected_grantee_capabilities: SelectedGranteeCapabilities,
};

type EntityShareActionsType = RefluxActions<{
  prepare: (GRN, ?EntitySharePayload) => Promise<EntityShareState>,
  update: (GRN, EntitySharePayload) => Promise<EntityShareState>,
  searchPaginatedUserShares: (username: string, page: number, perPage: number, query: string) => Promise<PaginatedUserSharesType>,
}>;

type EntityShareStoreType = Store<EntityShareStoreState>;

const defaultPreparePayload = {};

export const EntityShareActions: EntityShareActionsType = singletonActions(
  'permissions.EntityShare',
  () => Reflux.createActions({
    prepare: { asyncResult: true },
    update: { asyncResult: true },
  }),
);

export const EntityShareStore: EntityShareStoreType = singletonStore(
  'permissions.EntityShare',
  () => Reflux.createStore({
    listenables: [EntityShareActions],

    state: undefined,

    getInitialState(): EntityShareStoreState {
      return this._state();
    },

    prepare(entityGRN: GRN, payload: EntitySharePayload = defaultPreparePayload): Promise<EntityShareState> {
      const url = qualifyUrl(ApiRoutes.EntityShareController.prepare(entityGRN).url);
      const promise = fetch('POST', url, JSON.stringify(payload)).then(this._handleResponse);

      EntityShareActions.prepare.promise(promise);

      return promise;
    },

    update(entityGRN: GRN, payload: EntitySharePayload): Promise<EntityShareState> {
      const url = qualifyUrl(ApiRoutes.EntityShareController.update(entityGRN).url);
      const promise = fetch('POST', url, JSON.stringify(payload)).then(this._handleResponse);

      EntityShareActions.update.promise(promise);

      return promise;
    },

    searchPaginatedUserShares(username: string, page: number, perPage: number, query: string): Promise<PaginatedUserSharesType> {
      // const url = PaginationURL(ApiRoutes.EntityShareController.userSharesPaginated(username).url, page, perPage, query);
      // const promise = fetch('GET', qualifyUrl(url)).then((response: PaginatedUserSharesResponse) => {
      //   return {
      //     list: Immutable.List(response.entities.map((se) => ShareEntity.fromJSON(se))),
      //     context: {
      //       userCapabilities: response.context.user_capabilities,
      //     },
      //     pagination: {
      //       count: response.count,
      //       total: response.total,
      //       page: response.page,
      //       perPage: response.per_page,
      //       query: response.query,
      //     },
      //   };
      // });

      const mockedResponse = {
        total: 1,
        count: 1,
        page: page || 1,
        per_page: perPage || 10,
        query: query || '',
        entities: [
          {
            id: 'grn::::stream:57bc9188e62a2373778d9e03',
            type: 'stream',
            title: 'Security Data',
            owners: [
              {
                id: 'grn::::user:jane',
                type: 'user',
                title: 'Jane Doe',
              },
            ],
          },
        ],
        context: {
          user_capabilities: {
            'grn::::stream:57bc9188e62a2373778d9e03': 'view',
          },
        },
      };

      const promise = Promise.resolve({
        list: Immutable.List(mockedResponse.entities.map((se) => ShareEntity.fromJSON(se))),
        context: { userCapabilities: mockedResponse.context.user_capabilities },
        pagination: {
          count: mockedResponse.count,
          total: mockedResponse.total,
          page: mockedResponse.page,
          perPage: mockedResponse.per_page,
          query: mockedResponse.query,
        },
      });
      EntityShareActions.searchPaginatedUserShares.promise(promise);

      return promise;
    },

    _handleResponse(entityShareStateJSON: EntityShareStateJson): EntityShareState {
      const entityShareState = EntityShareState.fromJSON(entityShareStateJSON);

      this.state = entityShareState;

      this._trigger();

      return this.state;
    },

    _state(): EntityShareStoreState {
      return {
        state: this.state,
      };
    },

    _trigger() {
      this.trigger(this._state());
    },
  }),
);

export default EntityShareStore;
