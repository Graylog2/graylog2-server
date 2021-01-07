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
import { useContext } from 'react';
import { render, screen, fireEvent, waitFor } from 'wrappedTestingLibrary';
import { useLocation } from 'react-router-dom';

import WidgetFocusProvider from 'views/components/contexts/WidgetFocusProvider';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';

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

jest.mock('stores/connect', () => ({
  useStore: jest.fn(() => ({
    has: jest.fn(() => true),
  })),
}));

describe('WidgetFocusProvider', () => {
  const renderSUT = () => {
    const Consumer = () => {
      const { setFocusedWidget, focusedWidget } = useContext(WidgetFocusContext);

      return (
        <>
          <button type="button" onClick={() => setFocusedWidget('click')}>Click</button>
          <div>{focusedWidget}</div>
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

  it('should set focused widget from url', () => {
    useLocation.mockReturnValue({
      pathname: '',
      search: 'focused=clack',
    });

    renderSUT();
    const div = screen.getByText('clack');

    expect(div).not.toBeEmptyDOMElement();
  });
});
