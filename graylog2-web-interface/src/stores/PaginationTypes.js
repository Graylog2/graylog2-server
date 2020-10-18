// @flow strict

export type PaginatedResponseType = {
  count: number,
  total: number,
  page: number,
  per_page: number,
  query: string,
};

export type PaginationType = {
  count: $PropertyType<PaginatedResponseType, 'count'>,
  total: $PropertyType<PaginatedResponseType, 'total'>,
  page: $PropertyType<PaginatedResponseType, 'page'>,
  perPage: $PropertyType<PaginatedResponseType, 'per_page'>,
  query: $PropertyType<PaginatedResponseType, 'query'>,
};

export type PaginatedListJSON = {
  page: number,
  per_page: number,
  total: number,
  count: number,
  query: string,
};

export type PaginatedList = {
  pagination: {
    page: number,
    perPage: number,
    query: string,
  },
  total: number,
  count: number,
};
