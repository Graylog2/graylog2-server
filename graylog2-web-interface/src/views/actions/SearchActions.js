// @flow strict
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

export type CreateSearchResponse = {
  search: Search,
};

export type SearchId = string;

export type SearchExecutionResult = {
  result: SearchResult,
  widgetMapping: WidgetMapping,
};

type SearchActionsType = RefluxActions<{
  create: (Search) => Promise<CreateSearchResponse>,
  execute: (SearchExecutionState) => Promise<SearchExecutionResult>,
  reexecuteSearchTypes: (
    executionState: SearchExecutionState,
    searchTypes: {[searchTypeId: string]: { limit: number, offset: number }},
    effectiveTimeRange?: TimeRange,
  ) => Promise<SearchExecutionResult>,
  executeWithCurrentState: () => Promise<SearchExecutionResult>,
  refresh: () => Promise<void>,
  get: (SearchId) => Promise<SearchJson>,
  parameters: ((Array<Parameter> | Immutable.List<Parameter>)) => Promise<View>,
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
  }),
);

export default SearchActions;
