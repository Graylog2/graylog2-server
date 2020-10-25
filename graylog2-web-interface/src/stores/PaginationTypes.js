// @flow strict
import * as Immutable from 'immutable';

import type { AdditionalQueries } from 'util/PaginationURL';

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

export type Pagination = {
  page: number,
  perPage: number,
  query: string,
  additionalQueries?: AdditionalQueries,
};

export type PaginatedListJSON = {
  page: $PropertyType<Pagination, 'page'>,
  per_page: $PropertyType<Pagination, 'perPage'>,
  query: $PropertyType<Pagination, 'query'>,
  total: number,
  count: number,
};

export type ListPagination = Pagination & {
    total: number,
    count: number,
};

export type PaginatedList<ItemType> = {
  list: Immutable.List<ItemType>,
  pagination: ListPagination,
};
