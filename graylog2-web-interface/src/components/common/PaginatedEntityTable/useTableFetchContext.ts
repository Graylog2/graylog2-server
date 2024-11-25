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
import { useContext } from 'react';

import TableFetchContext from './TableFetchContext';
import type { ContextValue } from './TableFetchContext';

const useTableFetchContext = (skip: boolean = false) => {
  const tableFetchContext = useContext(TableFetchContext);

  if (skip) return {} as ContextValue;

  if (!tableFetchContext) {
    throw new Error('useTableFetchContext hook needs to be used inside TableFetchContext.Provider');
  }

  return tableFetchContext;
};

export default useTableFetchContext;
