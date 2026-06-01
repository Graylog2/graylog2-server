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
import Immutable from 'immutable';
import { useMemo } from 'react';

import useCreateSavedSearch from 'views/logic/views/UseCreateSavedSearch';
import { useSearchURLQueryParams } from 'views/logic/NormalizeSearchURLQueryParams';
import useCreateSearch from 'views/hooks/useCreateSearch';
import useQuery from 'routing/useQuery';
import Store from 'logic/local-storage/Store';
import Parameter from 'views/logic/parameters/Parameter';
import type { ParameterBindingJsonRepresentation } from 'views/logic/parameters/ParameterBinding';
import ParameterBinding from 'views/logic/parameters/ParameterBinding';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import type { SearchFilter } from 'views/types';

import SearchPage from './SearchPage';

const useReplayStateFromStore = () => {
  const { 'session-id': sessionId } = useQuery();

  return useMemo(() => {
    if (sessionId) {
      const searchDataFromStore = Store.get<string>(sessionId as string);
      Store.delete(sessionId as string);

      const searchData = searchDataFromStore ? JSON.parse(searchDataFromStore) : undefined;

      if (searchData) {
        return {
          parameters: searchData.parameters?.map((param) => Parameter.fromJSON(param)),
          parameterBindings: searchData.parameterBindings
            ? Immutable.Map<string, ParameterBinding>(
                Object.entries<ParameterBindingJsonRepresentation>(searchData.parameterBindings).map(
                  ([paramName, paramBinding]) => [paramName, ParameterBinding.fromJSON(paramBinding)],
                ),
              )
            : undefined,
          searchFilters: searchData.filters as SearchFilter[] | undefined,
        };
      }
    }

    return { parameters: undefined, parameterBindings: undefined, searchFilters: undefined };
  }, [sessionId]);
};

const NewSearchPage = () => {
  const { parameters, parameterBindings, searchFilters } = useReplayStateFromStore();
  const { timeRange, queryString, streams, streamCategories } = useSearchURLQueryParams();
  const viewPromise = useCreateSavedSearch({
    streamId: streams,
    streamCategory: streamCategories,
    timeRange,
    queryString,
    parameters,
    searchFilters,
  });
  const view = useCreateSearch(viewPromise);

  return <SearchPage view={view} executionState={SearchExecutionState.create(parameterBindings)} isNew />;
};

export default NewSearchPage;
