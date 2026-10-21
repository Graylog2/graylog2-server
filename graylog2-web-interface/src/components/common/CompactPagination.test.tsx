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
import userEvent from '@testing-library/user-event';

import selectEvent from 'helpers/selectEvent';

import CompactPagination from './CompactPagination';

describe('<CompactPagination />', () => {
  it('displays the current page and the total pages', async () => {
    render(<CompactPagination currentPage={3} totalPages={5} />);

    await screen.findByText('3');
    await screen.findByText('of 5');
  });

  it('does not render when there is only one page', () => {
    render(<CompactPagination currentPage={1} totalPages={1} />);

    expect(screen.queryByTestId('graylog-compact-pagination')).not.toBeInTheDocument();
  });

  it('disables the previous button on the first page', async () => {
    render(<CompactPagination currentPage={1} totalPages={5} />);

    expect(await screen.findByRole('button', { name: /open previous page/i })).toBeDisabled();
    expect(screen.getByRole('button', { name: /open next page/i })).toBeEnabled();
  });

  it('disables the next button on the last page', async () => {
    render(<CompactPagination currentPage={5} totalPages={5} />);

    expect(await screen.findByRole('button', { name: /open next page/i })).toBeDisabled();
    expect(screen.getByRole('button', { name: /open previous page/i })).toBeEnabled();
  });

  it('calls `onChange` with the previous and next page', async () => {
    const onChange = jest.fn();
    render(<CompactPagination currentPage={3} totalPages={5} onChange={onChange} />);

    await userEvent.click(await screen.findByRole('button', { name: /open previous page/i }));

    expect(onChange).toHaveBeenLastCalledWith(2);

    await userEvent.click(screen.getByRole('button', { name: /open next page/i }));

    expect(onChange).toHaveBeenLastCalledWith(4);
  });

  it('calls `onChange` with the page selected in the dropdown', async () => {
    const onChange = jest.fn();
    render(<CompactPagination currentPage={1} totalPages={5} onChange={onChange} />);

    await selectEvent.chooseOption('Select page', '4');

    expect(onChange).toHaveBeenLastCalledWith(4);
  });

  it('calls `onChange` with the page typed into the select', async () => {
    const onChange = jest.fn();
    render(<CompactPagination currentPage={1} totalPages={50} onChange={onChange} />);

    await userEvent.type(await screen.findByRole('combobox', { name: /select page/i }), '25{enter}');

    expect(onChange).toHaveBeenLastCalledWith(25);
  });
});
