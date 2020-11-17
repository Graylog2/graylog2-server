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
// @flow strict
import * as React from 'react';
import { render, act, screen } from 'wrappedTestingLibrary';
import { alertsManager as exampleRole } from 'fixtures/roles';

import RoleEdit from './RoleEdit';

jest.mock('./UsersSection', () => () => <div>UsersSection</div>);

jest.useFakeTimers();

describe('RoleEdit', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should display loading indicator, if no role is provided', async () => {
    render(<RoleEdit role={undefined} />);

    act(() => {
      jest.advanceTimersByTime(200);
    });

    await screen.findByText('Loading...');
  });

  it('should display role profile', () => {
    render(<RoleEdit role={exampleRole} />);

    expect(screen.getByText(exampleRole.name)).toBeInTheDocument();
    expect(screen.getByText(exampleRole.description)).toBeInTheDocument();
  });

  it('should display users section', () => {
    render(<RoleEdit role={exampleRole} />);

    expect(screen.getByText('UsersSection')).toBeInTheDocument();
  });
});
