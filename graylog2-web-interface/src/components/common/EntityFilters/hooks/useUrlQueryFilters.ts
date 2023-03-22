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
  }, {}), [urlQueryFilters]); // use immutable ordered map

  const setFilterValues = (newFilters: UrlQueryFilters) => {
    const newUrlQueryFilters = Object.entries(newFilters).reduce((col, [attributeId, filters]) => (
      [...col, ...filters.map((value) => `${attributeId}=${value}`)]
    ), []);

    setUrlQueryFilters(newUrlQueryFilters);
  };

  return [filtersFromQuery, setFilterValues]; // make sure data has same order as urlQueryFilters
};

export default useUrlQueryFilters;
