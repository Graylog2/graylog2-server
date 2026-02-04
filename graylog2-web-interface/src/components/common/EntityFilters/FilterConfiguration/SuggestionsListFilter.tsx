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
import * as React from 'react';
import { useState } from 'react';

import type { Filters } from 'components/common/EntityFilters/types';
import type { Attribute } from 'stores/PaginationTypes';
import useFilterValueSuggestions from 'components/common/EntityFilters/hooks/useFilterValueSuggestions';

import SuggestionsList from './SuggestionsList';

type Props = {
  allActiveFilters: Filters | undefined;
  attribute: Attribute;
  filterValueRenderer: (value: unknown, title: string) => React.ReactNode | undefined;
  multiSelect: boolean;
  onSubmit: (filter: { title: string; value: string }, closeDropdown: boolean) => void;
};

const DEFAULT_SEARCH_PARAMS = {
  query: '',
  pageSize: 10,
  page: 1,
};

const SuggestionsListFilter = ({ attribute, filterValueRenderer, onSubmit, allActiveFilters, multiSelect }: Props) => {
  const [searchParams, setSearchParams] = useState(DEFAULT_SEARCH_PARAMS);
  const {
    data: { pagination, suggestions },
    isInitialLoading,
  } = useFilterValueSuggestions(
    attribute.id,
    attribute.related_collection,
    attribute.related_identifier,
    searchParams,
    attribute.related_property,
    attribute.related_display_fields,
    attribute.related_display_template,
  );

  return (
    <SuggestionsList
      isLoading={isInitialLoading}
      total={pagination.total}
      pageSize={searchParams.pageSize}
      page={searchParams.page}
      setSearchParams={setSearchParams}
      allActiveFilters={allActiveFilters}
      attribute={attribute}
      multiSelect={multiSelect}
      filterValueRenderer={filterValueRenderer}
      onSubmit={onSubmit}
      suggestions={suggestions}
    />
  );
};

export default SuggestionsListFilter;
