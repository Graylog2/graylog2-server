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

import PaginatedEntityTableFilterContext, { type TableFilterContextValue } from './TableFilterContext';

type Props = React.PropsWithChildren<{
  externalSearch?: {
    query: string;
  };
  value: TableFilterContextValue;
}>;

const noop = () => {};

const TableFilterProvider = ({ children = undefined, externalSearch = undefined, value }: Props) => {
  const contextValue = useMemo(() => {
    if (!externalSearch) {
      return value;
    }

    return {
      ...value,
      searchParams: {
        ...value.searchParams,
        query: externalSearch.query,
      },
      setQuery: noop,
    };
  }, [externalSearch, value]);

  return (
    <PaginatedEntityTableFilterContext.Provider value={contextValue}>
      {children}
    </PaginatedEntityTableFilterContext.Provider>
  );
};

export default TableFilterProvider;
