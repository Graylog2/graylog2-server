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
import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';

import { Events } from '@graylog/server-api';

import type { FilterComponentProps } from 'stores/PaginationTypes';
import SuggestionsList from 'components/common/EntityFilters/FilterConfiguration/SuggestionsList';

const DEFAULT_SEARCH_PARAMS = {
  query: '',
  pageSize: 10,
  page: 1,
};

// Suggestions come from a 30d window, matching AssociatedAssetsFilter's parity. A follow-up
// could narrow this to the table's active timerange via useTableFetchContext + parseTimerangeFilter.
const TIMERANGE_30D = { type: 'relative', range: 30 * 24 * 60 * 60 };

// Hard cap on rendered suggestions; relies on the search box to narrow further.
const MAX_RENDERED_SUGGESTIONS = 100;

type Suggestion = { id: string; value: string };

const emptyFilter = {
  alerts: 'include' as const,
  extra_filters: {},
  aggregation_timerange: { type: 'relative', range: 0 },
  id: [] as string[],
  priority: [] as string[],
  event_definitions: [] as string[],
  key: [] as string[],
};

const fetchTagSuggestions = (): Promise<Suggestion[]> =>
  Events.slices({
    include_all: true,
    slice_column: 'tags',
    query: '',
    filter: emptyFilter,
    timerange: TIMERANGE_30D,
  }).then((response) =>
    (response?.slices ?? [])
      .map((slice) => slice?.value)
      .filter((value): value is string => typeof value === 'string' && value.length > 0)
      .sort()
      .map((value) => ({ id: value, value })),
  );

const TagsFilter = ({ attribute, allActiveFilters, filter, filterValueRenderer, onSubmit }: FilterComponentProps) => {
  const [searchParams, setSearchParams] = useState(DEFAULT_SEARCH_PARAMS);
  const { data: allSuggestions, isInitialLoading } = useQuery({
    queryKey: ['events', 'tag-suggestions'],
    queryFn: fetchTagSuggestions,
  });

  const suggestions = useMemo(
    () =>
      allSuggestions
        ? allSuggestions
            .filter((s) => s.value.toLowerCase().includes(searchParams.query.toLowerCase()))
            .slice(0, MAX_RENDERED_SUGGESTIONS)
        : allSuggestions,
    [allSuggestions, searchParams.query],
  );

  return (
    <SuggestionsList
      allActiveFilters={allActiveFilters}
      attribute={attribute}
      multiSelect={!filter}
      filterValueRenderer={filterValueRenderer}
      onSubmit={onSubmit}
      suggestions={suggestions}
      isLoading={isInitialLoading}
      total={suggestions?.length ?? 0}
      page={1}
      pageSize={suggestions?.length ?? 0}
      setSearchParams={setSearchParams}
    />
  );
};

export default TagsFilter;
