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

enum ViewActionsLayoutOptions {
  FULL_MENU = 'FULL_MENU',
  SAVE_COPY = 'SAVE_COPY',
  BLANK = 'BLANK'
}

export type LayoutState = {
  sidebar: { isShown: boolean }
  viewActionsLayoutOptions: ViewActionsLayoutOptions
}

type SeacrchPageLayoutProviderProps = {
  children: React.ReactNode
  providerOverrides?: LayoutState
}

const defaultState = {
  sidebar: { isShown: true },
  viewActionsLayoutOptions: ViewActionsLayoutOptions.FULL_MENU,
} as LayoutState;

const SearchPageLayoutContext = React.createContext<LayoutState | undefined>(undefined);

function SearchPageLayoutProvider({ children, providerOverrides = defaultState }: SeacrchPageLayoutProviderProps) {
  const [state] = React.useState<LayoutState>(providerOverrides);

  const value = React.useMemo(() => ({ ...state }), [state]);

  return (
    <SearchPageLayoutContext.Provider value={value}>
      {children}
    </SearchPageLayoutContext.Provider>
  );
}

SearchPageLayoutProvider.defaultProps = {
  providerOverrides: defaultState,
};

function useSearchPageLayout() {
  const context = React.useContext(SearchPageLayoutContext);

  if (context === undefined) {
    throw new Error('useSearchPageConfig must be used within a SearchPageConfigContextProvider');
  }

  return context;
}

export { SearchPageLayoutContext, SearchPageLayoutProvider, useSearchPageLayout, ViewActionsLayoutOptions };
