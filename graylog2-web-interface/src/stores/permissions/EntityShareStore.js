// @flow strict
import Reflux from 'reflux';

import ApiRoutes from 'routing/ApiRoutes';
import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import type { RefluxActions, Store } from 'stores/StoreTypes';
import { singletonActions, singletonStore } from 'views/logic/singleton';
import type { EntityShareUpdateRequest, EntitySharePrepareRequest, GRN } from 'logic/permissions/types';
import EntityShareState, { type EntityShareStateJson } from 'logic/permissions/EntityShareState';

type EntityShareStoreState = {
  entityShareState: EntityShareState,
};

type EntityShareActionsType = RefluxActions<{
  prepare: (GRN, EntitySharePrepareRequest) => Promise<EntityShareState>,
  update: (GRN, EntityShareUpdateRequest) => Promise<EntityShareState>,
}>;

type EntityShareStoreType = Store<EntityShareStoreState>;

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
    entityShareState: undefined,

    getInitialState(): EntityShareStoreState {
      return this._state();
    },

    prepare(entityGRN: GRN, entityShareRequest: EntitySharePrepareRequest): Promise<EntityShareState> {
      const url = qualifyUrl(ApiRoutes.EntityShareController.prepare(entityGRN));
      const promise = fetch('POST', url, JSON.stringify(entityShareRequest)).then(this._handleResponse);

      EntityShareActions.prepare.promise(promise);

      return promise;
    },

    update(entityGRN: GRN, entityShareRequest: EntityShareUpdateRequest): Promise<EntityShareState> {
      const url = qualifyUrl(ApiRoutes.EntityShareController.update(entityGRN));
      const promise = fetch('POST', url, JSON.stringify(entityShareRequest)).then(this._handleResponse);

      EntityShareActions.prepare.promise(promise);

      return promise;
    },

    _handleResponse(entityShareStateJSON: EntityShareStateJson): EntityShareState {
      const entityShareState = EntityShareState.fromJSON(entityShareStateJSON);

      this.entityShareState = entityShareState;

      this._trigger();

      return this.entityShareState;
    },

    _state(): EntityShareStoreState {
      return {
        entityShareState: this.entityShareState,
      };
    },

    _trigger() {
      this.trigger(this.entityShareState);
    },
  }),
);
