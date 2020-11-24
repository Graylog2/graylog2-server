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
