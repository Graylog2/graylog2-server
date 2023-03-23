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
import { useQueryParam, ArrayParam } from 'use-query-params';
import { useMemo } from 'react';

import type { UrlQueryFilters } from 'components/common/EntityFilters/types';

const useUrlQueryFilters = (): [UrlQueryFilters, (filters: UrlQueryFilters) => void] => {
  const [urlQueryFilters, setUrlQueryFilters] = useQueryParam('filters', ArrayParam);

  const filtersFromQuery = useMemo(() => (urlQueryFilters ?? []).reduce((col, filter) => {
    const [filterKey, filterValue] = filter.split(/=(.*)/);

    return {
      ...col,
      [filterKey]: [...(col[filterKey] ?? []), filterValue],
    };
  }, {}), [urlQueryFilters]);

  const setFilterValues = (newFilters: UrlQueryFilters) => {
    const newUrlQueryFilters = Object.entries(newFilters).reduce((col, [attributeId, filters]) => (
      [...col, ...filters.map((value) => `${attributeId}=${value}`)]
    ), []);

    setUrlQueryFilters(newUrlQueryFilters);
  };

  return [filtersFromQuery, setFilterValues];
};

export default useUrlQueryFilters;
