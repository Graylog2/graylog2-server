// @flow strict
import Reflux from 'reflux';

import type { RefluxActions } from 'stores/StoreTypes';
import type { GRN, UserSharedEntities } from 'logic/permissions/types';
import { type AdditionalQueries } from 'util/PaginationURL';
import { singletonActions } from 'views/logic/singleton';
import EntityShareState, { type SelectedGranteeCapabilities } from 'logic/permissions/EntityShareState';

type UserSharesPaginationType = {
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

const EntityShareActions: EntityShareActionsType = singletonActions(
  'permissions.EntityShare',
  () => Reflux.createActions({
    prepare: { asyncResult: true },
    update: { asyncResult: true },
    searchPaginatedUserShares: { asyncResult: true },
  }),
);

export default EntityShareActions;
