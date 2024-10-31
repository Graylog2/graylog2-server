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
import { render, screen } from 'wrappedTestingLibrary';
import { fireEvent } from '@testing-library/react';

import SortIcon from 'components/common/SortIcon';
import Direction from 'views/logic/aggregationbuilder/Direction';

const onChange = jest.fn();

describe('SortIcon', () => {
  it('has correct classes for asc direction', async () => {
    render(<SortIcon onChange={onChange} activeDirection={Direction.Ascending.direction} />);
    const Icon = await screen.findByTestId('sort-icon-svg');
    const Button = await screen.findByRole('button');

    expect(Button).toHaveClass('active');
    expect(Icon).toHaveClass('sort-icon-asc');
  });

  it('has correct classes for desc direction', async () => {
    render(<SortIcon onChange={onChange} activeDirection={Direction.Descending.direction} />);
    const Icon = await screen.findByTestId('sort-icon-svg');
    const Button = await screen.findByRole('button');

    expect(Button).toHaveClass('active');
    expect(Icon).toHaveClass('sort-icon-desc');
  });

  it('has correct classes for inactive state', async () => {
    render(<SortIcon onChange={onChange} activeDirection={null} />);
    const Icon = await screen.findByTestId('sort-icon-svg');
    const Button = await screen.findByRole('button');

    expect(Button).not.toHaveClass('active');
    expect(Icon).toHaveClass('sort-icon-desc');
  });

  it('trigger onChange with current direction on click', async () => {
    render(<SortIcon onChange={onChange} activeDirection={Direction.Ascending.direction} />);
    const Button = await screen.findByRole('button');

    fireEvent.click(Button);

    await expect(onChange).toHaveBeenCalledWith(Direction.Ascending.direction);
  });

  it('has correct classes for asc direction with custom asc and dsc ids', async () => {
    render(<SortIcon onChange={onChange} activeDirection="asc" ascId="asc" descId="desc" />);
    const Icon = await screen.findByTestId('sort-icon-svg');
    const Button = await screen.findByRole('button');

    expect(Button).toHaveClass('active');
    expect(Icon).toHaveClass('sort-icon-asc');
  });

  it('has correct classes for desc direction with custom asc and dsc ids', async () => {
    render(<SortIcon onChange={onChange} activeDirection="desc" ascId="asc" descId="desc" />);
    const Icon = await screen.findByTestId('sort-icon-svg');
    const Button = await screen.findByRole('button');

    expect(Button).toHaveClass('active');
    expect(Icon).toHaveClass('sort-icon-desc');
  });
});
