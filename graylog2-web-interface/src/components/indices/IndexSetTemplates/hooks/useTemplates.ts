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
import { useQuery } from '@tanstack/react-query';

import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import type { Attribute, SearchParams } from 'stores/PaginationTypes';
import PaginationURL from 'util/PaginationURL';
import FiltersForQueryParams from 'components/common/EntityFilters/FiltersForQueryParams';
import type {
  IndexSetTemplate,
} from 'components/indices/IndexSetTemplates/types';
import { defaultOnError } from 'util/conditional/onError';

const INITIAL_DATA = {
  pagination: { total: 0 },
  list: [],
  attributes: [],
};

export const fetchIndexSetTemplates = async (searchParams: SearchParams) => {
  const indexSetTemplateUrl = qualifyUrl('/system/indices/index_sets/templates/paginated');
  const url = PaginationURL(
    indexSetTemplateUrl,
    searchParams.page,
    searchParams.pageSize,
    searchParams.query,
    { filters: FiltersForQueryParams(searchParams.filters), sort: searchParams.sort.attributeId, order: searchParams.sort.direction });

  return fetch('GET', url).then(
    ({ elements, total, attributes }) => ({
      list: elements,
      pagination: { total },
      attributes,
    }));
};

export const keyFn = (searchParams: SearchParams) => (['indexSetTemplates', searchParams]);

const useTemplates = (searchParams: SearchParams, { enabled } = { enabled: true }): {
  data: {
    list: Readonly<Array<IndexSetTemplate>>,
    pagination: { total: number },
    attributes: Array<Attribute>
  },
  isLoading: boolean,
  refetch: () => void,
} => {
  const { data, isLoading, refetch } = useQuery(
    keyFn(searchParams),
    () => defaultOnError(fetchIndexSetTemplates(searchParams), 'Loading index set templates failed with status', 'Could not load index set templates'),
    {
      keepPreviousData: true,
      enabled,
    },
  );

  return ({
    data: data ?? INITIAL_DATA,
    isLoading,
    refetch,
  });
};

export default useTemplates;
