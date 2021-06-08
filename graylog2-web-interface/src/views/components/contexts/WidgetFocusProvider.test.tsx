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
import { Map } from 'immutable';
import { render } from 'wrappedTestingLibrary';
import { useLocation } from 'react-router-dom';
import { asMock } from 'helpers/mocking';

import { WidgetStore } from 'views/stores/WidgetStore';
import WidgetFocusProvider from 'views/components/contexts/WidgetFocusProvider';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';
import SearchActions from 'views/actions/SearchActions';

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

jest.mock('views/stores/WidgetStore', () => ({
  WidgetStore: {
    getInitialState: jest.fn(() => ({ has: jest.fn((widgetId) => widgetId === 'widget-id') })),
    listen: jest.fn(),
  },
}));

jest.mock('views/actions/SearchActions');

describe('WidgetFocusProvider', () => {
  beforeEach(() => {
    useLocation.mockReturnValue({
      pathname: '',
      search: '',
    });
  });

  const renderSUT = (consume) => render(
    <WidgetFocusProvider>
      <WidgetFocusContext.Consumer>
        {consume}
      </WidgetFocusContext.Consumer>
    </WidgetFocusProvider>,
  );

  it('should update url on widget focus', () => {
    let contextValue;
    const consume = (value) => { contextValue = value; };

    renderSUT(consume);

    contextValue.setWidgetFocusing('widget-id');

    expect(mockHistoryReplace).toBeCalledWith('?focusedId=widget-id&focusing=true');
  });

  it('should update url on widget focus close', () => {
    useLocation.mockReturnValueOnce({
      pathname: '',
      search: '?focusedId=widget-id&focusing=true',
    });

    let contextValue;
    const consume = (value) => { contextValue = value; };
    renderSUT(consume);

    contextValue.unsetWidgetFocusing();

    expect(mockHistoryReplace).toBeCalledWith('');
  });

  it('should set widget focus based on url', () => {
    useLocation.mockReturnValue({
      pathname: '',
      search: '?focusedId=widget-id&focusing=true',
    });

    let contextValue;
    const consume = (value) => { contextValue = value; };
    renderSUT(consume);

    expect(contextValue.focusedWidget).toEqual({ id: 'widget-id', focusing: true, editing: false });
  });

  it('should update url on widget edit', () => {
    let contextValue;
    const consume = (value) => { contextValue = value; };
    renderSUT(consume);

    contextValue.setWidgetEditing('widget-id');

    expect(mockHistoryReplace).toBeCalledWith('?focusedId=widget-id&editing=true');
  });

  it('should update url on widget edit close', () => {
    useLocation.mockReturnValue({
      pathname: '',
      search: '?focusedId=widget-id&editing=true',
    });

    let contextValue;
    const consume = (value) => { contextValue = value; };

    renderSUT(consume);

    contextValue.unsetWidgetEditing();

    expect(mockHistoryReplace).toBeCalledWith('');
  });

  it('should set widget edit and focused based on url', () => {
    useLocation.mockReturnValue({
      pathname: '',
      search: '?focusedId=widget-id&editing=true',
    });

    let contextValue;
    const consume = (value) => { contextValue = value; };
    renderSUT(consume);

    expect(contextValue.focusedWidget).toEqual({ id: 'widget-id', editing: true, focusing: true });
  });

  it('should not remove focus query param on widget edit', () => {
    useLocation.mockReturnValue({
      pathname: '',
      search: '?focusedId=widget-id&focusing=true',
    });

    let contextValue;
    const consume = (value) => { contextValue = value; };
    renderSUT(consume);

    contextValue.setWidgetEditing('widget-id');

    expect(mockHistoryReplace).toBeCalledWith('?focusedId=widget-id&focusing=true&editing=true');

    contextValue.unsetWidgetEditing();

    expect(mockHistoryReplace).toBeCalledWith('?focusedId=widget-id&focusing=true');
  });

  it('should not set focused widget from url and cleanup url if the widget does not exist', () => {
    asMock(WidgetStore.getInitialState).mockReturnValue(Map());

    useLocation.mockReturnValue({
      pathname: '',
      search: '?focusedId=not-existing-widget-id',
    });

    let contextValue;
    const consume = (value) => { contextValue = value; };
    renderSUT(consume);

    expect(contextValue.focusedWidget).toBe(undefined);

    expect(mockHistoryReplace).toBeCalledWith('');
  });

  it('should not trigger search execution when not leaving widget editing', async () => {
    useLocation.mockReturnValue({
      pathname: '',
      search: '',
    });

    const consume = jest.fn();

    renderSUT(consume);

    expect(SearchActions.executeWithCurrentState).not.toHaveBeenCalled();
  });
});
