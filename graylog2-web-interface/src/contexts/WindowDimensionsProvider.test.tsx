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
import { render, waitFor } from 'wrappedTestingLibrary';

import WindowDimensionsContextProvider from './WindowDimensionsContextProvider';
import WindowDimensionsContext from './WindowDimensionsContext';

const renderSUT = () => {
  const consume = jest.fn();

  render(
    <WindowDimensionsContextProvider>
      <WindowDimensionsContext.Consumer>
        {consume}
      </WindowDimensionsContext.Consumer>
    </WindowDimensionsContextProvider>,
  );

  return consume;
};

describe('WindowDimensionsProvider', () => {
  const setWindowProperty = (propertyName, value) => Object.defineProperty(window, propertyName, { writable: true, configurable: true, value });
  const setWindowWith = (width) => setWindowProperty('innerWidth', width);
  const setWindowHeight = (height) => setWindowProperty('innerHeight', height);

  it('should provider window dimensions', () => {
    setWindowWith(1000);
    setWindowHeight(500);
    const contextValue = renderSUT();

    expect(contextValue).toHaveBeenCalledWith({ width: 1000, height: 500 });
  });

  it('should update provided window dimensions on resize', async () => {
    setWindowWith(1000);
    setWindowHeight(500);

    const contextValue = renderSUT();

    setWindowWith(1200);
    setWindowHeight(600);

    window.dispatchEvent(new Event('resize'));

    await waitFor(() => expect(contextValue).toHaveBeenCalledWith({ width: 1200, height: 600 }));
  });
});
