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

import Badge from './Badge';

describe('Badge', () => {
  it('renders legacy bsStyle content unchanged', () => {
    render(<Badge bsStyle="danger">Alert</Badge>);

    expect(screen.getByText('Alert')).toBeInTheDocument();
  });

  it('renders as a button when onClick is set, with or without status', async () => {
    const onClick = jest.fn();
    render(
      <Badge status={{ color: 'success', variant: 'light' }} onClick={onClick}>
        Click me
      </Badge>,
    );

    await userEvent.click(screen.getByRole('button', { name: 'Click me' }));

    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it('renders status color/variant content', () => {
    render(<Badge status={{ color: 'success', variant: 'light' }}>Running</Badge>);

    expect(screen.getByText('Running')).toBeInTheDocument();
  });

  it('renders a dot indicator when status.dot is set', () => {
    render(
      <Badge status={{ color: 'success', variant: 'light', dot: true }}>
        Running
      </Badge>,
    );

    expect(screen.getByTestId('badge-dot')).toBeInTheDocument();
  });

  it('renders a left and right icon', () => {
    render(
      <Badge status={{ color: 'warning', variant: 'light' }} leftIcon="warning" rightIcon="pause">
        Paused
      </Badge>,
    );

    expect(screen.getByText('warning')).toHaveClass('material-symbols-rounded');
    expect(screen.getByText('pause')).toHaveClass('material-symbols-rounded');
  });
});
