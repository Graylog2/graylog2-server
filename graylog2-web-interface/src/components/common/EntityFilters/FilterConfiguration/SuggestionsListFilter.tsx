import * as React from 'react';
import { useState } from 'react';

import type { Filters, Filter } from 'components/common/EntityFilters/types';
import type { Attribute } from 'stores/PaginationTypes';
import useFilterValueSuggestions from 'components/common/EntityFilters/hooks/useFilterValueSuggestions';

import SuggestionsList from './SuggestionsList';

type Props = {
  allActiveFilters: Filters | undefined,
  attribute: Attribute,
  filter: Filter | undefined
  filterValueRenderer: (value: unknown, title: string) => React.ReactNode | undefined,
  onSubmit: (filter: { title: string, value: string }, closeDropdown: boolean) => void,
}

const DEFAULT_SEARCH_PARAMS = {
  query: '',
  pageSize: 10,
  page: 1,
};

const SuggestionsListFilter = ({ attribute, filterValueRenderer, onSubmit, allActiveFilters, filter }: Props) => {
  const [searchParams, setSearchParams] = useState(DEFAULT_SEARCH_PARAMS);
  const { data: { pagination, suggestions }, isInitialLoading } = useFilterValueSuggestions(attribute.id, attribute.related_collection, searchParams, attribute.related_property);

  return (
    <SuggestionsList isLoading={isInitialLoading}
                     total={pagination.total}
                     pageSize={searchParams.pageSize}
                     page={searchParams.page}
                     setSearchParams={setSearchParams}
                     allActiveFilters={allActiveFilters}
                     attribute={attribute}
                     filter={filter}
                     filterValueRenderer={filterValueRenderer}
                     onSubmit={onSubmit}
                     suggestions={suggestions} />
  );
};

export default SuggestionsListFilter;
