// @flow strict
import Reflux from 'reflux';

// import * as Immutable from 'immutable';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import permissionsMock from 'logic/permissions/mocked';
import EntityShareActions, { type EntitySharePayload, type PaginatedEnititySharesType } from 'actions/permissions/EntityShareActions';
import EntityShareState, { type EntityShareStateJson } from 'logic/permissions/EntityShareState';
import { qualifyUrl } from 'util/URLUtils';
import { singletonStore } from 'views/logic/singleton';
import { type AdditionalQueries } from 'util/PaginationURL';
import type { Store } from 'stores/StoreTypes';
import type { GRN } from 'logic/permissions/types';
// import type { GRN, SharedEntityType, UserSharedEntities } from 'logic/permissions/types';
// import PaginationURL, { type AdditionalQueries } from 'util/PaginationURL';

const DEFAULT_PREPARE_PAYLOAD = {};

type EntityShareStoreState = {
  state: EntityShareState,
};

type EntityShareStoreType = Store<EntityShareStoreState>;

const EntityShareStore: EntityShareStoreType = singletonStore(
  'permissions.EntityShare',
  () => Reflux.createStore({
    listenables: [EntityShareActions],

    state: undefined,

    getInitialState(): EntityShareStoreState {
      return this._state();
    },

    prepare(entityGRN: GRN, payload: EntitySharePayload = DEFAULT_PREPARE_PAYLOAD): Promise<EntityShareState> {
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

    searchPaginatedUserShares(username: string, page: number, perPage: number, query: string, additionalQueries?: AdditionalQueries): Promise<PaginatedEnititySharesType> {
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

      const promise = permissionsMock.searchPaginatedEntitySharesResponse(page, perPage, query, additionalQueries);
      EntityShareActions.searchPaginatedUserShares.promise(promise);

      return promise;
    },

    searchPaginatedTeamShares(teamId: string, page: number, perPage: number, query: string, additionalQueries?: AdditionalQueries): Promise<PaginatedEnititySharesType> {
      // Todo implmenet same code like for searchPaginatedUserShares, but with EntityShareController.teamSharesPaginated

      const promise = permissionsMock.searchPaginatedEntitySharesResponse(page, perPage, query, additionalQueries);
      EntityShareActions.searchPaginatedTeamShares.promise(promise);

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

export { EntityShareStore, EntityShareActions };
export default EntityShareStore;
