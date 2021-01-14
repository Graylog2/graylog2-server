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
import { useContext } from 'react';
import { render, screen, fireEvent, waitFor } from 'wrappedTestingLibrary';
import { useLocation } from 'react-router-dom';
import { asMock } from 'helpers/mocking';

import { WidgetStore } from 'views/stores/WidgetStore';
import WidgetFocusProvider from 'views/components/contexts/WidgetFocusProvider';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';
import Widget from 'views/logic/widgets/Widget';

const mockHistoryReplace = jest.fn();

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useHistory: () => ({
    replace: mockHistoryReplace,
  }),
  useLocation: jest.fn(() => ({
    pathname: '',
    search: '?focused=clack',
  })),
}));

jest.mock('views/stores/WidgetStore', () => ({
  WidgetStore: {
    getInitialState: jest.fn(() => ({ has: jest.fn(() => true) })),
    listen: jest.fn(),
  },
}));

describe('WidgetFocusProvider', () => {
  const renderSUT = () => {
    const Consumer = () => {
      const { setFocusedWidget, focusedWidget } = useContext(WidgetFocusContext);

      return (
        <>
          <button type="button" onClick={() => setFocusedWidget('click')}>Click</button>
          <div>{focusedWidget || 'No focus widget set'}</div>
        </>
      );
    };

    render(
      <WidgetFocusProvider>
        <Consumer />
      </WidgetFocusProvider>,
    );
  };

  it('should update url', async () => {
    renderSUT();
    const button = await screen.getByText('Click');
    fireEvent.click(button);

    await waitFor(() => {
      expect(mockHistoryReplace).toBeCalledWith('?focused=click');
    });
  });

  it('should set focused widget from url', async () => {
    asMock(WidgetStore.getInitialState).mockReturnValue(Map({ clack: Widget.builder().build() }));

    useLocation.mockReturnValue({
      pathname: '',
      search: 'focused=clack',
    });

    renderSUT();
    await screen.findByText('clack');
  });

  it('should not set focused widget from url if the widget does not exist', async () => {
    asMock(WidgetStore.getInitialState).mockReturnValue(Map());

    useLocation.mockReturnValue({
      pathname: '',
      search: 'focused=clack',
    });

    renderSUT();
    await screen.findByText('No focus widget set');
  });
});
