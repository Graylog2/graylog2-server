// @flow strict
import * as Immutable from 'immutable';

import SharedEntity from 'logic/permissions/SharedEntity';
import { type AdditionalQueries } from 'util/PaginationURL';

// Temporary file to mock api responses

const searchPaginatedUserSharesResponse = (page: number, perPage: number, query: string, additionalQueries?: AdditionalQueries) => {
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
      user_capabilities: {
        'grn::::stream:57bc9188e62a2373778d9e03': 'view',
      },
    },
  };

  return Promise.resolve({
    list: Immutable.List<any>(mockedResponse.entities.map((se) => SharedEntity.fromJSON(se))),
    context: { userCapabilities: mockedResponse.context.user_capabilities },
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

const availabeEntityTypes = {
  stream: 'Stream',
  dashboard: 'Dashboard',
  saved_search: 'Saved Search',
};

const availableCapabilities = {
  own: 'Owner',
  view: 'Viewer',
  manage: 'Manager',
};

export default { searchPaginatedUserSharesResponse, availabeEntityTypes, availableCapabilities };
