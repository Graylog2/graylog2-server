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
import { useMemo, useCallback } from 'react';
import { OrderedMap } from 'immutable';

import type { UrlQueryFilters } from 'components/common/EntityFilters/types';

const useUrlQueryFilters = (): [UrlQueryFilters, (filters: UrlQueryFilters) => void] => {
  const [pureUrlQueryFilters, setPureUrlQueryFilters] = useQueryParam('filters', ArrayParam);

  const filtersFromQuery = useMemo(() => (pureUrlQueryFilters ?? []).reduce((col, filter) => {
    const [attributeId, filterValue] = filter.split(/=(.*)/);

    return col.set(attributeId, [...(col.get(attributeId) ?? []), filterValue]);
  }, OrderedMap<string, Array<string>>()), [pureUrlQueryFilters]);

  const setFilterValues = useCallback((newFilters: UrlQueryFilters) => {
    const newPureUrlQueryFilters = newFilters.entrySeq().reduce((col, [attributeId, filters]) => (
      [...col, ...filters.map((value) => `${attributeId}=${value}`)]
    ), []);

    setPureUrlQueryFilters(newPureUrlQueryFilters);
  }, [setPureUrlQueryFilters]);

  return [filtersFromQuery, setFilterValues];
};

export default useUrlQueryFilters;
