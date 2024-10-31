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

import { createSearch } from 'fixtures/searches';
import PluggableStoreProvider from 'components/PluggableStoreProvider';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';

const TestStoreProvider = ({ children, undoRedoState, ...rest }: React.PropsWithChildren<Partial<React.ComponentProps<typeof PluggableStoreProvider>>>) => {
  const view = rest.view ?? createSearch();
  const isNew = rest.isNew ?? false;
  const initialQuery = rest.initialQuery ?? 'query-id-1';
  const executionState = rest.executionState ?? SearchExecutionState.empty();

  return (
    <PluggableStoreProvider undoRedoState={undoRedoState} view={view} initialQuery={initialQuery} isNew={isNew} executionState={executionState}>
      {children}
    </PluggableStoreProvider>
  );
};

export default TestStoreProvider;
