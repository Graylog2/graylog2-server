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
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import asMock from 'helpers/mocking/AsMock';
import AppConfig from 'util/AppConfig';

import HideOnCloud from './HideOnCloud';

jest.mock('util/AppConfig', () => ({
  isCloud: jest.fn(() => false),
}));

describe('HideOnCloud', () => {
  it('does not display children on cloud', () => {
    asMock(AppConfig.isCloud).mockReturnValue(true);

    render(<HideOnCloud>The Content</HideOnCloud>);

    expect(screen.queryByText('The Content')).not.toBeInTheDocument();
  });

  it('displays children when not on cloud', () => {
    asMock(AppConfig.isCloud).mockReturnValue(false);

    render(<HideOnCloud>The Content</HideOnCloud>);

    expect(screen.getByText('The Content')).toBeInTheDocument();
  });

  it('forwards props to its children', () => {
    const onClick = jest.fn();
    asMock(AppConfig.isCloud).mockReturnValue(false);

    render(<HideOnCloud onClick={onClick}><button type="button">Click Me!</button></HideOnCloud>);

    const childrenButton = screen.getByText('Click Me!');
    userEvent.click(childrenButton);

    expect(onClick).toHaveBeenCalledTimes(1);
  });
});
