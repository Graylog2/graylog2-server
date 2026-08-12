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
import { useQuery, keepPreviousData } from '@tanstack/react-query';

import { Events } from '@graylog/server-api';

import type { FilterComponentProps } from 'stores/PaginationTypes';
import SuggestionsList from 'components/common/EntityFilters/FilterConfiguration/SuggestionsList';
import useQuery_ from 'routing/useQuery';
import useDebouncedValue from 'hooks/useDebouncedValue';
import { MISSING_BUCKET_NAME } from 'views/Constants';

const DEFAULT_SEARCH_PARAMS = {
  query: '',
  pageSize: 10,
  page: 1,
};

// 30d window matches AssociatedAssetsFilter parity. Surfaces tags seen on events in the last 30d.
// Defined-but-unfired tags won't appear here; that's an accepted limitation.
const TIMERANGE_30D = { type: 'relative', range: 30 * 24 * 60 * 60 };

// Hard cap on rendered suggestions; relies on the search box to narrow further.
const MAX_RENDERED_SUGGESTIONS = 100;

// Debounce window for forwarding the typed prefix to a server-side fetcher.
const PREFIX_DEBOUNCE_MS = 300;

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

const fetchTagSuggestions = (streamId: string | undefined, _prefix?: string): Promise<Suggestion[]> =>
  Events.slices({
    include_all: true,
    slice_column: 'tags',
    query: streamId ? `source_streams:${streamId}` : '',
    filter: emptyFilter,
    timerange: TIMERANGE_30D,
  }).then((response) =>
    (response?.slices ?? [])
      .map((slice) => slice?.value)
      // Drop the scripting-API missing-bucket placeholder — events without tags would otherwise
      // surface as a literal "(Empty Value)" suggestion, which isn't a real tag to filter by.
      .filter(
        (value): value is string => typeof value === 'string' && value.length > 0 && value !== MISSING_BUCKET_NAME,
      )
      .sort()
      .map((value) => ({ id: value, value })),
  );

type FetchSuggestions = (streamId: string | undefined, prefix?: string) => Promise<Suggestion[]>;

/**
 * Build a `filter_component` for the tags column. Different consumers use different sources of
 * truth (indexed events vs. defined event definitions) so the fetcher is injected.
 *
 * Set `serverSidePrefix` when the backend caps results (e.g. the event-definitions
 * suggest endpoint, which is hard-capped at 100). In that mode the debounced search-box
 * query is forwarded to the fetcher as a prefix and included in the React Query key so
 * later keystrokes can reach entries past the server cap.
 */
export const createTagsFilter = ({
  queryKeyPrefix,
  fetchSuggestions,
  streamScoped = false,
  serverSidePrefix = false,
}: {
  queryKeyPrefix: ReadonlyArray<string>;
  fetchSuggestions: FetchSuggestions;
  streamScoped?: boolean;
  serverSidePrefix?: boolean;
}) => {
  const Filter = ({ attribute, allActiveFilters, filter, filterValueRenderer, onSubmit }: FilterComponentProps) => {
    const [searchParams, setSearchParams] = useState(DEFAULT_SEARCH_PARAMS);
    const [debouncedQuery] = useDebouncedValue(searchParams.query, PREFIX_DEBOUNCE_MS);
    const { stream_id: streamId } = useQuery_();
    const streamIdParam = streamScoped && typeof streamId === 'string' ? streamId : undefined;
    const prefixParam = serverSidePrefix ? debouncedQuery : undefined;
    const {
      data: allSuggestions,
      isInitialLoading,
      isError,
    } = useQuery({
      queryKey: [...queryKeyPrefix, streamIdParam ?? 'all', ...(serverSidePrefix ? [prefixParam] : [])],
      queryFn: () => fetchSuggestions(streamIdParam, prefixParam),
      placeholderData: serverSidePrefix ? keepPreviousData : undefined,
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

    // Surface a fetch failure as an empty list so the user isn't left wondering why no
    // suggestions appear. The dropdown's "No results" state already handles the visual.
    const effectiveSuggestions = isError ? [] : suggestions;

    return (
      <SuggestionsList
        allActiveFilters={allActiveFilters}
        attribute={attribute}
        multiSelect={!filter}
        filterValueRenderer={filterValueRenderer}
        onSubmit={onSubmit}
        suggestions={effectiveSuggestions}
        isLoading={isInitialLoading}
        total={effectiveSuggestions?.length ?? 0}
        page={1}
        pageSize={effectiveSuggestions?.length ?? 0}
        setSearchParams={setSearchParams}
      />
    );
  };

  return Filter;
};

const TagsFilter = createTagsFilter({
  queryKeyPrefix: ['events', 'tag-suggestions', `t${TIMERANGE_30D.range}`],
  fetchSuggestions: fetchTagSuggestions,
  streamScoped: true,
});

export default TagsFilter;
