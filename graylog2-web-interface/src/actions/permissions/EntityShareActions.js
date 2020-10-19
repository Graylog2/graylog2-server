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
  loadUserSharesPaginated: (username: string, pagination: Pagination) => Promise<PaginatedEntityShares>,
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
