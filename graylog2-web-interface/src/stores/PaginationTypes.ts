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
import type * as Immutable from 'immutable';

import type { AdditionalQueries } from 'util/PaginationURL';
import type { UrlQueryFilters, Filter, Filters } from 'components/common/EntityFilters/types';

export type PaginatedResponseType = {
  count: number,
  total: number,
  page: number,
  per_page: number,
  query: string | null,
};

export type PaginatedListJSON = {
  page: Pagination['page'],
  per_page: Pagination['perPage'],
  query: Pagination['query'],
  total: number,
  count: number,
};

export type Pagination = {
  page: number,
  perPage: number,
  query: string,
  additionalQueries?: AdditionalQueries,
};

export type ListPagination = Pagination & {
  total: number,
  count: number,
};

export const DEFAULT_PAGINATION: Pagination = {
  page: 1,
  perPage: 10,
  query: '',
};

export type PaginatedList<ItemType> = {
  list: Immutable.List<ItemType>,
  pagination: ListPagination,
};

export type Sort = {
  attributeId: string,
  direction: 'asc' | 'desc'
};

export type SearchParams = {
  page: number,
  pageSize: number,
  query: string,
  sort: Sort
  filters?: UrlQueryFilters
}

export type FilterComponentProps = {
  attribute: Attribute,
  filter?: Filter,
  filterValueRenderer: (value: Filter['value'], title: string) => React.ReactNode | undefined,
  onSubmit: (filter: { title: string, value: string }, closeDropdown?: boolean) => void,
  allActiveFilters: Filters | undefined,
}
export type Attribute = {
  id: string,
  title: string,
  type?: 'BOOLEAN' | 'STRING' | 'DATE' | 'OBJECT_ID',
  sortable?: boolean,
  hidden?: boolean,
  searchable?: boolean,
  filterable?: true,
  filter_options?: Array<{ value: string, title: string }>,
  filter_component?: React.ComponentType<FilterComponentProps>,
  related_collection?: string,
  related_property?: string,
  permissions?: Array<string>,
}

export type Attributes = Array<Attribute>
