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
import { useEffect, useMemo } from 'react';
import debounce from 'lodash/debounce';
import { Map } from 'immutable';

import Query from 'views/logic/queries/Query';
import Search from 'views/logic/search/Search';
import { useStore } from 'stores/connect';
import { isPermitted } from 'util/PermissionsMixin';
import { FilterPreviewActions, FilterPreviewStore } from 'stores/event-definitions/FilterPreviewStore';
import generateId from 'logic/generateId';
import type { EventDefinition } from 'components/event-definitions/event-definitions-types';
import type User from 'logic/users/User';
import useCurrentUser from 'hooks/useCurrentUser';

import FilterPreview from './FilterPreview';

const isPermittedToSeePreview = (currentUser: User, config: EventDefinition['config']) => {
  const missingPermissions = config?.streams?.some(
    (stream) => !isPermitted(currentUser.permissions, `streams:read:${stream}`),
  );

  return !missingPermissions;
};

const fetchSearch = debounce(
  (config: EventDefinition['config'], searchTypeId: string, queryId: string, currentUser: User) => {
    if (!isPermittedToSeePreview(currentUser, config)) {
      return;
    }

    const formattedStreams = config?.streams?.map((stream) => ({ type: 'stream', id: stream })) || [];

    const queryBuilder = Query.builder()
      .id(queryId)
      .query({ type: 'elasticsearch', query_string: config?.query || '*' })
      .timerange({ type: 'relative', range: (config?.search_within_ms || 0) / 1000 })
      .filter(formattedStreams.length === 0 ? null : Map({ type: 'or', filters: formattedStreams }))
      .filters(config.filters)
      .searchTypes([
        {
          id: searchTypeId,
          type: 'messages',
          limit: 10,
          offset: 0,
          filter: undefined,
          filters: undefined,
          name: undefined,
          query: undefined,
          timerange: undefined,
          streams: [],
          stream_categories: [],
          sort: [],
          decorators: [],
        },
      ]);

    const query = queryBuilder.build();

    const search = Search.create()
      .toBuilder()
      .parameters(config?.query_parameters?.filter((param) => !param.embryonic) || [])
      .queries([query])
      .build();

    FilterPreviewActions.search(search);
  },
  250,
);

type FilterPreviewContainerProps = {
  eventDefinition: EventDefinition;
};
const FilterPreviewContainer = ({ eventDefinition }: FilterPreviewContainerProps) => {
  const queryId = useMemo(() => generateId(), []);
  const searchTypeId = useMemo(() => generateId(), []);
  const currentUser = useCurrentUser();
  const filterPreview = useStore(FilterPreviewStore);

  useEffect(() => {
    fetchSearch(eventDefinition.config, searchTypeId, queryId, currentUser);
  }, [currentUser, eventDefinition.config, queryId, searchTypeId]);

  const isLoading = !filterPreview?.result?.forId(queryId);
  let searchResult;
  let errors;

  if (!isLoading) {
    searchResult = filterPreview.result.forId(queryId).searchTypes[searchTypeId];

    errors = filterPreview.result.errors; // result may not always be set, so I can't use destructuring
  }

  return (
    <FilterPreview
      isFetchingData={isLoading}
      displayPreview={isPermittedToSeePreview(currentUser, eventDefinition.config)}
      searchResult={searchResult}
      errors={errors}
    />
  );
};

export default FilterPreviewContainer;
