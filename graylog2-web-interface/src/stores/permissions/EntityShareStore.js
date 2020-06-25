// @flow strict
import Reflux from 'reflux';

import ApiRoutes from 'routing/ApiRoutes';
import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import type { RefluxActions, Store } from 'stores/StoreTypes';
import { singletonActions, singletonStore } from 'views/logic/singleton';
import type { EntityShareRequest, GRN } from 'logic/permissions/types';
import EntityShareState from 'logic/permissions/EntityShareState';

type EntityShareStoreState = {
  entityShareState: EntityShareState,
};

type EntityShareActionsType = RefluxActions<{
  prepare: (EntityShareRequest) => Promise<EntityShareState>,
  update: (EntityShareRequest) => Promise<EntityShareState>,
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

    prepare(entityGRN: GRN, entityShareRequest: EntityShareRequest): Promise<EntityShareState> {
      const url = qualifyUrl(ApiRoutes.EntityShareController.prepare(entityGRN));
      const promise = fetch('POST', url, JSON.stringify(entityShareRequest))
        .then(EntityShareState.fromJSON)
        .then((entityShareState) => {
          this.entityShareState = entityShareState;

          this._trigger();

          return this.entityShareState;
        });

      EntityShareActions.prepare.promise(promise);

      return promise;
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
