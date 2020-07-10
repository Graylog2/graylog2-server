// @flow strict
import * as React from 'react';

type SearchPageLayoutConfig = {
  sidebar: {
    dashboardSidebarIsPinned: boolean,
    searchSidebarIsPinned: boolean,
    isPinned: () => boolean,
  },
};

export type SearchPageLayout = {
  config: SearchPageLayoutConfig,
  actions: { toggleSidebarPinning: () => void },
};

const SearchPageLayoutContext = React.createContext<?SearchPageLayout>();

export default SearchPageLayoutContext;
