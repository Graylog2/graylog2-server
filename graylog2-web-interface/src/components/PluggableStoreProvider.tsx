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
import { Provider } from 'react-redux';
import * as Immutable from 'immutable';

import usePluginEntities from 'hooks/usePluginEntities';
import createStore from 'store';
import type View from 'views/logic/views/View';
import type { QueryId } from 'views/logic/queries/Query';
import type { QuerySet } from 'views/logic/search/Search';
import type SearchExecutionState from 'views/logic/search/SearchExecutionState';
import type { UndoRedoState } from 'views/logic/slices/undoRedoSlice';
import type { RootState } from 'views/types';
import useSearchExecutors from 'views/components/contexts/useSearchExecutors';

type Props = {
  initialQuery: QueryId,
  isNew: boolean,
  view: View,
  executionState: SearchExecutionState,
  undoRedoState?: UndoRedoState,
  result?: RootState['searchExecution']['result'],
}

const useActiveQuery = (initialQuery: Props['initialQuery'], view: Props['view']) => useMemo(() => {
  const queries: QuerySet = view?.search?.queries ?? Immutable.Set();

  if (initialQuery && queries.find((q) => q.id === initialQuery) !== undefined) {
    return initialQuery;
  }

  return queries.first()?.id;
}, [initialQuery, view?.search?.queries]);

const useInitialState = (undoRedoState: UndoRedoState, view: View, isNew: boolean, activeQuery, executionState: SearchExecutionState, result?: Props['result']): Partial<RootState> => useMemo(() => {
  const undoRedo = undoRedoState ? { undoRedo: undoRedoState } : {};

  return ({
    view: {
      view,
      isDirty: false,
      isNew,
      activeQuery,
    },
    searchExecution: {
      widgetsToSearch: undefined,
      executionState,
      isLoading: false,
      result,
    },
    ...undoRedo,
  });
},
// eslint-disable-next-line react-hooks/exhaustive-deps
[executionState, isNew, view]);

const PluggableStoreProvider = ({ initialQuery, children, isNew, view, executionState, undoRedoState, result }: React.PropsWithChildren<Props>) => {
  const reducers = usePluginEntities('views.reducers');
  const activeQuery = useActiveQuery(initialQuery, view);
  const initialState = useInitialState(undoRedoState, view, isNew, activeQuery, executionState, result);
  const searchExecutors = useSearchExecutors();
  const store = useMemo(() => createStore(reducers, initialState, searchExecutors), [initialState, reducers, searchExecutors]);

  return (
    <Provider store={store}>
      {children}
    </Provider>
  );
};

PluggableStoreProvider.defaultProps = {
  undoRedoState: undefined,
  result: undefined,
};

export default PluggableStoreProvider;
