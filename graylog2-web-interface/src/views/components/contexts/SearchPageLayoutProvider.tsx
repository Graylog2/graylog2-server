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

import type { LayoutState } from 'views/components/contexts/SearchPageLayoutContext';
import SearchPageLayoutContext, { FULL_MENU } from 'views/components/contexts/SearchPageLayoutContext';

type SearchPageLayoutProviderProps = {
  children: React.ReactNode
  readonly providerOverrides?: LayoutState
}

export const defaultState = {
  sidebar: { isShown: true },
  viewActions: FULL_MENU,
} as LayoutState;

function SearchPageLayoutProvider({ children, providerOverrides = defaultState }: SearchPageLayoutProviderProps) {
  const value = providerOverrides;

  return (
    <SearchPageLayoutContext.Provider value={value}>
      {children}
    </SearchPageLayoutContext.Provider>
  );
}

SearchPageLayoutProvider.defaultProps = {
  providerOverrides: defaultState,
};

export default SearchPageLayoutProvider;
