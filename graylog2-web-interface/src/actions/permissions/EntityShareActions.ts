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

import type { RefluxActions } from 'stores/StoreTypes';
import SharedEntity from 'logic/permissions/SharedEntity';
import type { GRN } from 'logic/permissions/types';
import type { PaginatedList, Pagination } from 'stores/PaginationTypes';
import { singletonActions } from 'views/logic/singleton';
import EntityShareState, { type SelectedGranteeCapabilities } from 'logic/permissions/EntityShareState';

export type PaginatedEntityShares = PaginatedList<SharedEntity> & {
  context: {
    granteeCapabilities: { [grn: GRN]: string },
  },
};

export type EntitySharePayload = {
  selected_grantee_capabilities: SelectedGranteeCapabilities,
};

export type ActionsType = {
  prepare: (entityType: string, entityTitle: string, GRN: GRN, payload: ?EntitySharePayload) => Promise<EntityShareState>,
  update: (entityType: string, entityTitle: string, GRN: GRN, payload: EntitySharePayload) => Promise<EntityShareState>,
  loadUserSharesPaginated: (userId: string, pagination: Pagination) => Promise<PaginatedEntityShares>,
};

type EntityShareActionsType = RefluxActions<ActionsType>;

const EntityShareActions: EntityShareActionsType = singletonActions(
  'permissions.EntityShare',
  () => Reflux.createActions({
    prepare: { asyncResult: true },
    update: { asyncResult: true },
    loadUserSharesPaginated: { asyncResult: true },
  }),
);

export default EntityShareActions;
