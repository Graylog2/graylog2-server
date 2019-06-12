// @flow strict
import Reflux from 'reflux';

import Search from 'views/logic/search/Search';
import SearchResult from 'views/logic/SearchResult';
import type { WidgetMapping } from 'views/logic/views/View';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import Parameter from 'views/logic/parameters/Parameter';
import View from 'views/logic/views/View';
import type { SearchJson } from 'views/logic/search/Search';

export type CreateSearchResponse = {
  search: Search,
};

export type SearchId = string;

export type SearchExecutionResult = {
  result: SearchResult,
  widgetMapping: WidgetMapping,
};

type SearchActionsType = {
  create: (Search) => Promise<CreateSearchResponse>,
  execute: (SearchExecutionState) => Promise<SearchExecutionResult>,
  executeWithCurrentState: () => Promise<SearchExecutionResult>,
  refresh: () => Promise<*>,
  get: (SearchId) => Promise<SearchJson>,
  parameters: (Array<Parameter>) => Promise<View>,
};

const SearchActions: SearchActionsType = Reflux.createActions({
  create: {
    asyncResult: true,
  },
  get: {
    asyncResult: true,
  },
  execute: {
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
});

export default SearchActions;
