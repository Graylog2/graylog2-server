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
import * as Immutable from 'immutable';

import SharedEntity from 'logic/permissions/SharedEntity';
import { AdditionalQueries } from 'util/PaginationURL';

// Temporary file to mock api responses

const searchPaginatedEntitySharesResponse = (page: number, perPage: number, query: string, additionalQueries?: AdditionalQueries) => {
  const mockedEntities = new Array(perPage).fill({
    id: 'grn::::stream:57bc9188e62a2373778d9e03',
    type: 'stream',
    title: 'Security Data',
    owners: [
      {
        id: 'grn::::user:jane',
        type: 'user',
        title: 'Jane Doe',
      },
    ],
  });

  const mockedResponse = {
    additionalQueries: additionalQueries,
    total: 230,
    count: Math.round(230 / perPage),
    page: page || 1,
    per_page: perPage || 10,
    query: query || '',
    entities: mockedEntities,
    context: {
      grantee_capabilities: {
        'grn::::stream:57bc9188e62a2373778d9e03': 'view',
      },
    },
  };

  return Promise.resolve({
    list: Immutable.List<any>(mockedResponse.entities.map((se) => SharedEntity.fromJSON(se))),
    context: { granteeCapabilities: mockedResponse.context.grantee_capabilities },
    pagination: {
      additionalQueries: mockedResponse.additionalQueries,
      count: mockedResponse.count,
      total: mockedResponse.total,
      page: mockedResponse.page,
      perPage: mockedResponse.per_page,
      query: mockedResponse.query,
    },
  });
};

const availableEntityTypes = {
  stream: 'Stream',
  dashboard: 'Dashboard',
  saved_search: 'Saved Search',
  event_definition: 'Event Definition',
};

const availableCapabilities = {
  own: 'Owner',
  view: 'Viewer',
  manage: 'Manager',
};

export default { searchPaginatedEntitySharesResponse, availableEntityTypes, availableCapabilities };
