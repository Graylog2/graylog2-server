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
// @flow strict
import Reflux from 'reflux';
import * as Immutable from 'immutable';

import SharedEntity from 'logic/permissions/SharedEntity';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import EntityShareState, { type EntityShareStateJson } from 'logic/permissions/EntityShareState';
import EntityShareActions, { type EntitySharePayload, type PaginatedEntityShares } from 'actions/permissions/EntityShareActions';
import { qualifyUrl } from 'util/URLUtils';
import { singletonStore } from 'views/logic/singleton';
import type { Store } from 'stores/StoreTypes';
import type { GRN } from 'logic/permissions/types';
import PaginationURL from 'util/PaginationURL';
import type { Pagination } from 'stores/PaginationTypes';

const DEFAULT_PREPARE_PAYLOAD = {};

type EntityShareStoreState = {
  state: EntityShareState,
};

type EntityShareStoreType = Store<EntityShareStoreState>;

const formatPaginatedSharesResponse = ({
  additional_queries: additionalQueries,
  context,
  count,
  entities,
  page,
  per_page: perPage,
  query,
  total,
}) => ({
  list: Immutable.List(entities.map((se) => SharedEntity.fromJSON(se))),
  context: {
    granteeCapabilities: context.grantee_capabilities,
  },
  pagination: {
    additionalQueries,
    page,
    perPage,
    query,
    count,
    total,
  },
});

const EntityShareStore: EntityShareStoreType = singletonStore(
  'permissions.EntityShare',
  () => Reflux.createStore({
    listenables: [EntityShareActions],

    state: undefined,

    getInitialState(): EntityShareStoreState {
      return this._state();
    },

    prepare(entityType: string, entityTitle: string, entityGRN: GRN, payload: EntitySharePayload = DEFAULT_PREPARE_PAYLOAD): Promise<EntityShareState> {
      const url = qualifyUrl(ApiRoutes.EntityShareController.prepare(entityGRN).url);
      const promise = fetch('POST', url, JSON.stringify(payload)).then(this._handleResponse);

      EntityShareActions.prepare.promise(promise);

      return promise;
    },

    update(entityType: string, entityTitle: string, entityGRN: GRN, payload: EntitySharePayload): Promise<EntityShareState> {
      const url = qualifyUrl(ApiRoutes.EntityShareController.update(entityGRN).url);
      const promise = fetch('POST', url, JSON.stringify(payload)).then(this._handleResponse);

      EntityShareActions.update.promise(promise);

      return promise;
    },

    loadUserSharesPaginated(userId: string, { page, perPage, query, additionalQueries }: Pagination): Promise<PaginatedEntityShares> {
      const url = PaginationURL(ApiRoutes.EntityShareController.userSharesPaginated(userId).url, page, perPage, query, additionalQueries);
      const promise = fetch('GET', qualifyUrl(url)).then(formatPaginatedSharesResponse);

      EntityShareActions.loadUserSharesPaginated.promise(promise);

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
