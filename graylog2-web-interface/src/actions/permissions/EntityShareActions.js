// @flow strict
import Reflux from 'reflux';

import type { RefluxActions } from 'stores/StoreTypes';
import type { GRN, SharedEntities } from 'logic/permissions/types';
import { type AdditionalQueries } from 'util/PaginationURL';
import { singletonActions } from 'views/logic/singleton';
import EntityShareState, { type SelectedGranteeCapabilities } from 'logic/permissions/EntityShareState';

type EntitySharesPaginationType = {
  count: number,
  total: number,
  page: number,
  perPage: number,
  query: string,
  additionalQueries?: AdditionalQueries,
};

export type PaginatedEnititySharesType = {
  list: SharedEntities,
  pagination: EntitySharesPaginationType,
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
  loadUserSharesPaginated: (username: string, page: number, perPage: number, query: string, additionalQueries?: AdditionalQueries) => Promise<PaginatedEnititySharesType>,
  loadTeamSharesPaginated: (username: string, page: number, perPage: number, query: string, additionalQueries?: AdditionalQueries) => Promise<PaginatedEnititySharesType>,
}>;

const EntityShareActions: EntityShareActionsType = singletonActions(
  'permissions.EntityShare',
  () => Reflux.createActions({
    prepare: { asyncResult: true },
    update: { asyncResult: true },
    loadUserSharesPaginated: { asyncResult: true },
    loadTeamSharesPaginated: { asyncResult: true },
  }),
);

export default EntityShareActions;
