// @flow strict
import Reflux from 'reflux';

// import * as Immutable from 'immutable';
import ApiRoutes from 'routing/ApiRoutes';
import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import type { RefluxActions, Store } from 'stores/StoreTypes';
import { singletonActions, singletonStore } from 'views/logic/singleton';
// import type { GRN, SharedEntityType, UserSharedEntities } from 'logic/permissions/types';
import type { GRN, UserSharedEntities } from 'logic/permissions/types';
// import PaginationURL, { type AdditionalQueries } from 'util/PaginationURL';
import permissionsMock from 'logic/permissions/mocked';
import { type AdditionalQueries } from 'util/PaginationURL';
import EntityShareState, { type EntityShareStateJson, type SelectedGranteeCapabilities } from 'logic/permissions/EntityShareState';

type EntityShareStoreState = {
  state: EntityShareState,
};

export type UserSharesPaginationType = {
  count: number,
  total: number,
  page: number,
  perPage: number,
  query: string,
  additionalQueries?: AdditionalQueries,
};

export type PaginatedUserSharesType = {
  list: UserSharedEntities,
  pagination: UserSharesPaginationType,
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
  searchPaginatedUserShares: (username: string, page: number, perPage: number, query: string, additionalQueries?: AdditionalQueries) => Promise<PaginatedUserSharesType>,
}>;

type EntityShareStoreType = Store<EntityShareStoreState>;

const defaultPreparePayload = {};

export const EntityShareActions: EntityShareActionsType = singletonActions(
  'permissions.EntityShare',
  () => Reflux.createActions({
    prepare: { asyncResult: true },
    update: { asyncResult: true },
    searchPaginatedUserShares: { asyncResult: true },
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

    searchPaginatedUserShares(username: string, page: number, perPage: number, query: string, additionalQueries?: AdditionalQueries): Promise<PaginatedUserSharesType> {
      // const url = PaginationURL(ApiRoutes.EntityShareController.userSharesPaginated(username).url, page, perPage, query, additionalQueries);
      // const promise = fetch('GET', qualifyUrl(url)).then((response: PaginatedUserSharesResponse) => {
      //   return {
      //     list: Immutable.List(response.entities.map((se) => SharedEntity.fromJSON(se))),
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

      const promise = permissionsMock.searchPaginatedUserSharesResponse(page, perPage, query, additionalQueries);
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
