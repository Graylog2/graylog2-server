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
import { render } from 'wrappedTestingLibrary';
import { useLocation } from 'react-router-dom';

import { asMock } from 'helpers/mocking';
import WidgetFocusProvider from 'views/components/contexts/WidgetFocusProvider';
import type { WidgetFocusContextType } from 'views/components/contexts/WidgetFocusContext';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';
import { loadViewsPlugin, unloadViewsPlugin } from 'views/test/testViewsPlugin';
import TestStoreProvider from 'views/test/TestStoreProvider';
import { allMessagesTable } from 'views/logic/Widgets';
import { createViewWithWidgets } from 'fixtures/searches';
import useAppDispatch from 'stores/useAppDispatch';

const mockHistoryReplace = jest.fn();

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useHistory: () => ({
    replace: mockHistoryReplace,
  }),
  useLocation: jest.fn(() => ({
    pathname: '',
    search: '',
  })),
}));

jest.mock('stores/useAppDispatch');

const emptyLocation = {
  pathname: '',
  search: '',
  hash: '',
  state: undefined,
};

jest.mock('views/logic/slices/searchExecutionSlice', () => ({
  ...jest.requireActual('views/logic/slices/searchExecutionSlice'),
  execute: jest.fn(() => async () => {}),
}));

describe('WidgetFocusProvider', () => {
  beforeAll(loadViewsPlugin);

  afterAll(unloadViewsPlugin);

  beforeEach(() => {
    const dispatch = jest.fn();
    asMock(useAppDispatch).mockReturnValue(dispatch);
    asMock(useLocation).mockReturnValue(emptyLocation);
  });

  const renderSUT = (consume: (value: WidgetFocusContextType) => JSX.Element) => {
    const widget = allMessagesTable('widget-id');
    const view = createViewWithWidgets([widget], {});

    return render(
      <TestStoreProvider view={view}>
        <WidgetFocusProvider>
          <WidgetFocusContext.Consumer>
            {consume}
          </WidgetFocusContext.Consumer>
        </WidgetFocusProvider>
      </TestStoreProvider>,
    );
  };

  it('should update url on widget focus', () => {
    let contextValue;

    const consume = (value: WidgetFocusContextType) => {
      contextValue = value;

      return null;
    };

    renderSUT(consume);

    contextValue.setWidgetFocusing('widget-id');

    expect(mockHistoryReplace).toHaveBeenCalledWith('?focusedId=widget-id&focusing=true');
  });

  it('should update url on widget focus close', () => {
    asMock(useLocation).mockReturnValueOnce({
      ...emptyLocation,
      search: '?focusedId=widget-id&focusing=true',
    });

    let contextValue;

    const consume = (value: WidgetFocusContextType) => {
      contextValue = value;

      return null;
    };

    renderSUT(consume);

    contextValue.unsetWidgetFocusing();

    expect(mockHistoryReplace).toHaveBeenCalledWith('');
  });

  it('should set widget focus based on url', () => {
    asMock(useLocation).mockReturnValue({
      ...emptyLocation,
      search: '?focusedId=widget-id&focusing=true',
    });

    const consume = jest.fn();

    renderSUT(consume);

    expect(consume).toHaveBeenCalledWith(expect.objectContaining({ focusedWidget: { id: 'widget-id', focusing: true, editing: false } }));
  });

  it('should update url on widget edit', () => {
    let contextValue;

    const consume = (value: WidgetFocusContextType) => {
      contextValue = value;

      return null;
    };

    renderSUT(consume);

    contextValue.setWidgetEditing('widget-id');

    expect(mockHistoryReplace).toHaveBeenCalledWith('?focusedId=widget-id&editing=true');
  });

  it('should update url on widget edit close', () => {
    asMock(useLocation).mockReturnValue({
      ...emptyLocation,
      search: '?focusedId=widget-id&editing=true',
    });

    let contextValue;

    const consume = (value: WidgetFocusContextType) => {
      contextValue = value;

      return null;
    };

    renderSUT(consume);

    contextValue.unsetWidgetEditing();

    expect(mockHistoryReplace).toHaveBeenCalledWith('');
  });

  it('should set widget edit and focused based on url', () => {
    asMock(useLocation).mockReturnValue({
      ...emptyLocation,
      search: '?focusedId=widget-id&editing=true',
    });

    const consume = jest.fn();

    renderSUT(consume);

    expect(consume).toHaveBeenCalledWith(expect.objectContaining({ focusedWidget: { id: 'widget-id', editing: true, focusing: true } }));
  });

  it('should not remove focus query param on widget edit', () => {
    asMock(useLocation).mockReturnValue({
      ...emptyLocation,
      search: '?focusedId=widget-id&focusing=true',
    });

    let contextValue;

    const consume = (value: WidgetFocusContextType) => {
      contextValue = value;

      return null;
    };

    renderSUT(consume);

    contextValue.setWidgetEditing('widget-id');

    expect(mockHistoryReplace).toHaveBeenCalledWith('?focusedId=widget-id&focusing=true&editing=true');

    contextValue.unsetWidgetEditing();

    expect(mockHistoryReplace).toHaveBeenCalledWith('?focusedId=widget-id&focusing=true');
  });

  it('should not set focused widget from url and cleanup url if the widget does not exist', () => {
    asMock(useLocation).mockReturnValue({
      ...emptyLocation,
      search: '?focusedId=not-existing-widget-id',
    });

    const consume = jest.fn();

    renderSUT(consume);

    expect(consume).toHaveBeenCalledWith(expect.objectContaining({ focusedWidget: undefined }));

    expect(mockHistoryReplace).toHaveBeenCalledWith('');
  });

  it('does not trigger setting widgets to search initially', () => {
    const dispatch = jest.fn();
    asMock(useAppDispatch).mockReturnValue(dispatch);
    asMock(useLocation).mockReturnValue(emptyLocation);
    renderSUT(jest.fn());

    expect(dispatch).not.toHaveBeenCalled();
  });
});
