// @flow strict
import * as React from 'react';

import View, { type ViewType } from 'views/logic/views/View';

type SearchPageLayoutConfig = {
  sidebar: {
    dashboardSidebarIsPinned: boolean,
    searchSidebarIsPinned: boolean,
    isPinned: (viewType: ?ViewType) => boolean,
  },
};

export type SearchPageLayout = {
  config: SearchPageLayoutConfig,
  actions: {toggleSidebarPinning: (viewType: ?$PropertyType<View, 'type'>) => void },
};

const SearchPageLayoutContext = React.createContext<?SearchPageLayout>();

export default SearchPageLayoutContext;
