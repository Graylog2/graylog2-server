// @flow strict
/* eslint-disable import/prefer-default-export */
import * as Immutable from 'immutable';

import Grantee from 'logic/permissions/Grantee';
import SharedEntity from 'logic/permissions/SharedEntity';
import { type AdditionalQueries } from 'util/PaginationURL';

export const paginatedShares = (page: number, perPage: number, query: string, additionalQueries?: AdditionalQueries) => {
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
  const sharedEnitites = new Array(perPage).fill(sharedEntity);

  return {
    list: Immutable.List<SharedEntity>(sharedEnitites),
    context: {
      granteeCapabilities: { 'grn::::stream:57bc9188e62a2373778d9e03': 'view' },
    },
    pagination: {
      additionalQueries,
      count: Math.round(230 / perPage),
      total: 230,
      page: page || 1,
      perPage: perPage || 10,
      query: query || '',
    },
  };
};
