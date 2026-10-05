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

import PlatformPicker from './PlatformPicker';

describe('PlatformPicker', () => {
  const onSelect = jest.fn();

  beforeEach(() => {
    onSelect.mockClear();
  });

  it('renders all five platform cards', () => {
    render(<PlatformPicker onSelect={onSelect} selectedPlatform={null} disabled={false} />);

    expect(screen.getByRole('button', { name: /linux/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /windows/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /macos/i })).toBeInTheDocument();
  });

  it('calls onSelect with the platform id when a card is clicked', async () => {
    render(<PlatformPicker onSelect={onSelect} selectedPlatform={null} disabled={false} />);

    await userEvent.click(screen.getByRole('button', { name: /linux/i }));

    expect(onSelect).toHaveBeenCalledWith('linux');
  });

  it('does not render clickable cards when disabled', () => {
    render(<PlatformPicker onSelect={onSelect} selectedPlatform={null} disabled />);

    expect(screen.queryByRole('button', { name: /linux/i })).not.toBeInTheDocument();
  });

  it('highlights the selected platform', () => {
    render(<PlatformPicker onSelect={onSelect} selectedPlatform="linux" disabled={false} />);

    expect(screen.getByText('check_circle')).toBeInTheDocument();
  });
});
