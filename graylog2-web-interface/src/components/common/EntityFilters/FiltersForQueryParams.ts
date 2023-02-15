import type { Filters } from 'components/common/EntityFilters/types';

// Transform filters, so they can be used as URL query params, for example for the PaginationURL helper.
const FiltersForQueryParams = (filters: Filters) => {
  if (!filters) {
    return undefined;
  }

  return Object.entries(filters).map(([attributeId, filterValues]) => (
    filterValues.map(({ value }) => `${attributeId}:${value}`)
  ));
};

export default FiltersForQueryParams;
