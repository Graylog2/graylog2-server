// @flow strict
/* eslint-disable import/prefer-default-export */
import * as Immutable from 'immutable';

import Grantee from 'logic/permissions/Grantee';
import SharedEntity from 'logic/permissions/SharedEntity';
import type { Pagination } from 'stores/PaginationTypes';

export const paginatedShares = ({ page, perPage, query, additionalQueries }: Pagination) => {
  const entityOwner = Grantee
    .builder()
    .id('grn::::user:jane')
    .type('user')
    .title('Jane Doe')
    .build();
  const sharedEntity = SharedEntity
    .builder()
    .id('grn::::stream:57bc9188e62a2373778d9e03')
    .type('stream')
    .title('Security Data')
    .owners(Immutable.List([entityOwner]))
    .build();
  const sharedEntities = new Array(perPage).fill(sharedEntity);

  return {
    list: Immutable.List<SharedEntity>(sharedEntities),
    context: {
      granteeCapabilities: { 'grn::::stream:57bc9188e62a2373778d9e03': 'view' },
    },
    pagination: {
      additionalQueries,
      page: page || 1,
      perPage: perPage || 10,
      query: query || '',
      count: Math.round(230 / perPage),
      total: 230,
    },
  };
};
