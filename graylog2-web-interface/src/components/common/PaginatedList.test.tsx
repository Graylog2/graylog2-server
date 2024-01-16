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
import { render, fireEvent, screen, waitFor } from 'wrappedTestingLibrary';
import type { Location } from 'history';

import useLocation from 'routing/useLocation';
import InteractiveContext from 'views/components/contexts/InteractiveContext';
import { asMock } from 'helpers/mocking';

import PaginatedList from './PaginatedList';

jest.mock('routing/useLocation', () => jest.fn(() => ({ search: '' })));

describe('PaginatedList', () => {
  it('should display Pagination', () => {
    const { getByText } = render(
      <PaginatedList totalItems={100}
                     onChange={() => {}}>
        <div>The list</div>
      </PaginatedList>,
    );

    expect(getByText('The list')).not.toBeNull();
    expect(getByText('1')).not.toBeNull();
  });

  it('should not dived by 0 if pageSize is 0 Pagination', () => {
    const { getByText } = render(
      <PaginatedList totalItems={100}
                     pageSize={0}
                     onChange={() => {}}>
        <div>The list</div>
      </PaginatedList>,
    );

    expect(getByText('The list')).not.toBeNull();
  });

  it('should not display Pagination, when context is not interactive', () => {
    const { queryByText } = render(
      <InteractiveContext.Provider value={false}>
        <PaginatedList totalItems={100} onChange={() => {}}>
          <div>The list</div>
        </PaginatedList>,
      </InteractiveContext.Provider>,
    );

    expect(queryByText('1')).toBeNull();
  });

  it('should reset current page on page size change', async () => {
    const onChangeStub = jest.fn();
    const { getByRole } = render(
      <PaginatedList totalItems={200}
                     onChange={onChangeStub}
                     activePage={3}>
        <div>The list</div>
      </PaginatedList>);

    fireEvent.click(getByRole('button', {
      name: /configure page size/i,
    }));

    fireEvent.click(screen.getByRole('menuitem', { name: /100/ }));

    expect(onChangeStub).toHaveBeenCalledWith(1, 100);

    await waitFor(() => {
      expect(screen.queryByRole('menuitem', { name: /100/ })).not.toBeInTheDocument();
    });
  });

  describe('with state based on URL query params', () => {
    it('should set <page> query parameter as active page', async () => {
      const currentPage = 4;

      asMock(useLocation).mockReturnValue({
        search: `?page=${currentPage}`,
      } as Location);

      const { findByTestId } = render(
        <PaginatedList totalItems={200}
                       onChange={() => {}}
                       activePage={3}>
          <div>The list</div>
        </PaginatedList>);

      const graylogPagination = await findByTestId('graylog-pagination');
      const activePageElement = graylogPagination.getElementsByClassName('active');

      expect(activePageElement).not.toBeNull();
      expect(activePageElement[0].textContent).toContain(`${currentPage}`);
    });
  });

  describe('with internal state', () => {
    it('should update active page, when prop changes', async () => {
      const { findByTestId, rerender } = render(
        <PaginatedList totalItems={200}
                       onChange={() => {}}
                       activePage={3}
                       useQueryParameter={false}>
          <div>The list</div>
        </PaginatedList>);

      const graylogPagination = await findByTestId('graylog-pagination');
      const activePageElement = graylogPagination.getElementsByClassName('active');

      expect(activePageElement[0].textContent).toContain('3');

      rerender(
        <PaginatedList totalItems={200}
                       onChange={() => {}}
                       activePage={1}
                       useQueryParameter={false}>
          <div>The list</div>
        </PaginatedList>,
      );

      await findByTestId('graylog-pagination');
      const newActivePageElement = graylogPagination.getElementsByClassName('active');

      expect(newActivePageElement[0].textContent).toContain('1');
    });
  });
});
