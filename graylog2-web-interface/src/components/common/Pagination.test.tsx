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
import { fireEvent, render, screen, within } from 'wrappedTestingLibrary';

import Pagination from './Pagination';

describe('<Pagination />', () => {
  it('should display Pagination', async () => {
    const currentPage = 1;
    const totalPages = 5;

    render(<Pagination currentPage={currentPage}
                       totalPages={totalPages} />);

    const activePage = await screen.findByTitle('Active page');

    await screen.findByTestId('graylog-pagination');
    await screen.findByRole('button', { name: /open page 5/i });

    expect(within(activePage).getByText(1)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /open page 6/i })).not.toBeInTheDocument();
  });

  it('should not render Pagination if only 1 page', () => {
    const currentPage = 1;
    const totalPages = 1;
    const { container } = render(<Pagination currentPage={currentPage}
                                             totalPages={totalPages} />);

    expect(container.firstChild).toBeNull();
  });

  it('should return proper page to `onChange`', () => {
    const currentPage = 2;
    const totalPages = 10;
    const onChangeSpy = jest.fn();

    render(<Pagination currentPage={currentPage}
                       totalPages={totalPages}
                       onChange={onChangeSpy} />);

    fireEvent.click(screen.getByLabelText('Open next page'));

    expect(onChangeSpy).toHaveBeenLastCalledWith(currentPage + 1);
  });

  it('should return previous page to `onChange`', () => {
    const currentPage = 2;
    const totalPages = 10;
    const onChangeSpy = jest.fn();

    render(<Pagination currentPage={currentPage}
                       totalPages={totalPages}
                       onChange={onChangeSpy} />);

    fireEvent.click(screen.getByLabelText('Open previous page'));

    expect(onChangeSpy).toHaveBeenLastCalledWith(currentPage - 1);
  });

  it('should return last page to `onChange`', () => {
    const currentPage = 2;
    const totalPages = 10;
    const onChangeSpy = jest.fn();

    render(<Pagination currentPage={currentPage}
                       totalPages={totalPages}
                       onChange={onChangeSpy} />);

    fireEvent.click(screen.getByLabelText('Open last page'));

    expect(onChangeSpy).toHaveBeenLastCalledWith(totalPages);
  });

  it('should return first page to `onChange`', () => {
    const currentPage = 2;
    const totalPages = 10;
    const onChangeSpy = jest.fn();

    render(<Pagination currentPage={currentPage}
                       totalPages={totalPages}
                       onChange={onChangeSpy} />);

    fireEvent.click(screen.getByLabelText('Open first page'));

    expect(onChangeSpy).toHaveBeenLastCalledWith(1);
  });
});
