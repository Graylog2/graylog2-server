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
import { render, screen, fireEvent, waitFor } from 'wrappedTestingLibrary';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import asMock from 'helpers/mocking/AsMock';
import TestStoreProvider from 'views/test/TestStoreProvider';
import { viewSliceReducer } from 'views/logic/slices/viewSlice';
import { searchExecutionSliceReducer } from 'views/logic/slices/searchExecutionSlice';
import ViewsBindings from 'views/bindings';
import { undoRedoSliceReducer } from 'views/logic/slices/undoRedoSlice';
import { testView2, undoRedoTestStore } from 'fixtures/undoRedo';
import UndoNavItem from 'views/components/sidebar/undo/UndoNavItem';
import mockDispatch from 'views/test/mockDispatch';
import type { RootState } from 'views/types';
import useAppDispatch from 'stores/useAppDispatch';
import { undo } from 'views/logic/slices/undoRedoActions';

jest.mock('stores/useAppDispatch');

jest.mock('views/logic/slices/undoRedoSlice', () => ({
  ...jest.requireActual('views/logic/slices/undoRedoSlice'),
  undo: jest.fn(() => Promise.resolve()),
}));

describe('<UndoNavItem />', () => {
  const RedoNavItemComponent = () => (
    <TestStoreProvider undoRedoState={undoRedoTestStore}>
      <UndoNavItem sidebarIsPinned={false} />
    </TestStoreProvider>
  );

  const dispatch = mockDispatch({ view: { view: testView2, activeQuery: 'query-id-1' } } as RootState);

  beforeEach(() => {
    asMock(useAppDispatch).mockReturnValue(dispatch);
  });

  beforeAll(() => {
    PluginStore.register(new PluginManifest({}, {
      ...ViewsBindings,
      'views.reducers': [
        { key: 'view', reducer: viewSliceReducer },
        { key: 'searchExecution', reducer: searchExecutionSliceReducer },
        { key: 'undoRedo', reducer: undoRedoSliceReducer },
      ],
    }));
  });

  it('Call Undo action on call', async () => {
    render(<RedoNavItemComponent />);
    const undoButton = await screen.findByLabelText('Undo');
    fireEvent.click(undoButton);

    await waitFor(() => {
      expect(undo).toHaveBeenCalled();
    });
  });
});
