// @flow strict
import Reflux from 'reflux';

import Search from 'enterprise/logic/search/Search';
import SearchResult from 'enterprise/logic/SearchResult';
import type { WidgetMapping } from 'enterprise/logic/views/View';
import SearchExecutionState from 'enterprise/logic/search/SearchExecutionState';
import Parameter from 'enterprise/logic/parameters/Parameter';
import View from 'enterprise/logic/views/View';
import type { SearchJson } from 'enterprise/logic/search/Search';

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
});

export default SearchActions;
