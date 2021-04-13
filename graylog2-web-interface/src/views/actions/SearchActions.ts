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
import Reflux from 'reflux';
import * as Immutable from 'immutable';

import Search from 'views/logic/search/Search';
import SearchResult from 'views/logic/SearchResult';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import Parameter from 'views/logic/parameters/Parameter';
import View from 'views/logic/views/View';
import type { SearchJson } from 'views/logic/search/Search';
import { singletonActions } from 'views/logic/singleton';
import type { RefluxActions } from 'stores/StoreTypes';
import type { TimeRange } from 'views/logic/queries/Query';
import type { WidgetMapping } from 'views/logic/views/types';
import type { SearchTypeOptions } from 'views/logic/search/GlobalOverride';

export type CreateSearchResponse = {
  search: Search,
};

export type SearchId = string;

export type SearchExecutionResult = {
  result: SearchResult,
  widgetMapping: WidgetMapping,
};

export type FilterSearchTypes = string[];

type SearchActionsType = RefluxActions<{
  create: (search: Search) => Promise<CreateSearchResponse>,
  execute: (state: SearchExecutionState) => Promise<SearchExecutionResult>,
  reexecuteSearchTypes: (
    searchTypes: SearchTypeOptions,
    effectiveTimeRange?: TimeRange,
  ) => Promise<SearchExecutionResult>,
  executeWithCurrentState: () => Promise<SearchExecutionResult>,
  refresh: () => Promise<void>,
  get: (searchId: SearchId) => Promise<SearchJson>,
  parameters: (parameters: (Array<Parameter> | Immutable.List<Parameter>)) => Promise<View>,
  setFilterSearchTypes: (FilterSearchTypes) => Promise<void>,
}>;

const SearchActions: SearchActionsType = singletonActions(
  'views.Search',
  () => Reflux.createActions({
    create: {
      asyncResult: true,
    },
    get: {
      asyncResult: true,
    },
    execute: {
      asyncResult: true,
    },
    reexecuteSearchTypes: {
      asyncResult: true,
    },
    executeWithCurrentState: {
      asyncResult: true,
    },
    parameters: {
      asyncResult: true,
    },
    refresh: {
      asyncResult: true,
    },
    setFilterSearchTypes: {
      asyncResult: false,
    },
  }),
);

export default SearchActions;
