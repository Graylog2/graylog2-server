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
