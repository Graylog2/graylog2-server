import * as React from 'react';

import type { SearchExecutors } from 'views/logic/slices/searchExecutionSlice';
import executeSearch from 'views/logic/slices/executeSearch';
import parseSearch from 'views/logic/slices/parseSearch';

const defaultSearchExecutors: SearchExecutors = {
  resultMapper: (r) => r,
  execute: executeSearch,
  parse: parseSearch,
};
const SearchExecutorsContext = React.createContext<SearchExecutors>(defaultSearchExecutors);
export default SearchExecutorsContext;
