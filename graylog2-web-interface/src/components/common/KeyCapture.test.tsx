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
import React from 'react';
import { render, fireEvent, screen } from 'wrappedTestingLibrary';

import KeyCapture from './KeyCapture';

describe('<KeyCapture />', () => {
  it('renders its children', () => {
    render(<KeyCapture keys={{ enter: () => {} }}><>The children</></KeyCapture>);

    expect(screen.getByText('The children')).toBeInTheDocument();
  });

  it('triggers function defined for a specific key on key press', () => {
    const onEnter = jest.fn();
    render(<KeyCapture keys={{ enter: onEnter }} />);

    fireEvent.keyDown(document.body, { key: 'Enter', which: 13 });

    expect(onEnter).toHaveBeenCalledTimes(1);
  });
});
