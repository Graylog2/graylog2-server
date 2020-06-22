// @flow strict
import * as React from 'react';

type SearchPageLayoutConfig = {
  sidebar: { isPinned: boolean },
};

export type SearchPageLayout = {
  config: SearchPageLayoutConfig,
  setConfig: SearchPageLayoutConfig => void,
};

const SearchPageLayoutContext = React.createContext<?SearchPageLayout>();

export default SearchPageLayoutContext;
