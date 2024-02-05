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
import { useMemo } from 'react';
import merge from 'lodash/merge';

import type { LayoutState } from './SearchPageLayoutContext';
import SearchPageLayoutContext, { DEFAULT_STATE } from './SearchPageLayoutContext';

type Props = {
  children: React.ReactNode,
  value: Partial<LayoutState>,
};

const SearchPageLayoutProvider = ({ children, value }: Props) => {
  const contextValue = useMemo(() => merge({}, DEFAULT_STATE, value), [value]);

  return (
    <SearchPageLayoutContext.Provider value={contextValue}>
      {children}
    </SearchPageLayoutContext.Provider>
  );
};

export default SearchPageLayoutProvider;
