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

export type SearchConfigState = {
  sidebar: { isShown: boolean }
  viewActionsLayoutOptions: ViewActionsLayoutOptions
}

type SeacrchPageConfigProviderProps = {
  children: React.ReactNode
  providerOverrides?: SearchConfigState
}

const defaultState = {
  sidebar: { isShown: true },
  viewActionsLayoutOptions: ViewActionsLayoutOptions.FULL_MENU,
} as SearchConfigState;

const SearchPageConfigContext = React.createContext<SearchConfigState | undefined>(undefined);

function SearchPageConfigContextProvider({ children, providerOverrides = defaultState }: SeacrchPageConfigProviderProps) {
  const [state] = React.useState<SearchConfigState>(providerOverrides);

  const value = React.useMemo(() => ({ ...state }), [state]);

  return (
    <SearchPageConfigContext.Provider value={value}>
      {children}
    </SearchPageConfigContext.Provider>
  );
}

SearchPageConfigContextProvider.defaultProps = {
  providerOverrides: defaultState,
};

function useSearchPageConfig() {
  const context = React.useContext(SearchPageConfigContext);

  if (context === undefined) {
    throw new Error('useSearchPageConfig must be used within a SearchPageConfigContextProvider');
  }

  return context;
}

export { SearchPageConfigContext, SearchPageConfigContextProvider, useSearchPageConfig, ViewActionsLayoutOptions };
