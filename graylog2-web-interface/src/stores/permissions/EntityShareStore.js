// @flow strict
import Reflux from 'reflux';
import * as Immutable from 'immutable';

import ApiRoutes from 'routing/ApiRoutes';
import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import type { RefluxActions, Store } from 'stores/StoreTypes';
import { singletonActions, singletonStore } from 'views/logic/singleton';
import type { GRN } from 'logic/permissions/types';
import EntityShareState, { type EntityShareStateJson, type SelectedGranteeRoles } from 'logic/permissions/EntityShareState';

type EntityShareStoreState = {
  state: EntityShareState,
};

type EntitySharePreparePayload = {|
  selected_grantee_roles?: SelectedGranteeRoles,
|};

type EntityShareUpdatePayload = {|
  grantee_roles?: SelectedGranteeRoles,
|};

type EntityShareActionsType = RefluxActions<{
  prepare: (GRN, ?EntitySharePreparePayload) => Promise<EntityShareState>,
  update: (GRN, EntityShareUpdatePayload) => Promise<EntityShareState>,
}>;

type EntityShareStoreType = Store<EntityShareStoreState>;

const defaultPreparePayload = {
  selected_grantee_roles: Immutable.Map(),
};

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

    prepare(entityGRN: GRN, payload: EntitySharePreparePayload = defaultPreparePayload): Promise<EntityShareState> {
      const url = qualifyUrl(ApiRoutes.EntityShareController.prepare(entityGRN).url);
      const promise = fetch('POST', url, JSON.stringify(payload)).then(this._handleResponse);

      EntityShareActions.prepare.promise(promise);

      return promise;
    },

    update(entityGRN: GRN, payload: EntityShareUpdatePayload): Promise<EntityShareState> {
      const url = qualifyUrl(ApiRoutes.EntityShareController.update(entityGRN).url);
      const promise = fetch('POST', url, JSON.stringify(payload)).then(this._handleResponse);

      EntityShareActions.update.promise(promise);

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
