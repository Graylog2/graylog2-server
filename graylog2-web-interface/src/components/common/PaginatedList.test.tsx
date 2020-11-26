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
import { render, fireEvent } from 'wrappedTestingLibrary';

import InteractiveContext from 'views/components/contexts/InteractiveContext';

import PaginatedList from './PaginatedList';

describe('PaginatedList', () => {
  it('should display Pagination', () => {
    const { getByText } = render(<PaginatedList totalItems={100} onChange={() => {}}><div>The list</div></PaginatedList>);

    expect(getByText('The list')).not.toBeNull();
    expect(getByText('1')).not.toBeNull();
  });

  it('should not dived by 0 if pageSize is 0 Pagination', () => {
    const { getByText } = render(<PaginatedList totalItems={100} pageSize={0} onChange={() => {}}><div>The list</div></PaginatedList>);

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

  it('should reset current page on page size change', () => {
    const onChangeStub = jest.fn();
    const { getByLabelText } = render(<PaginatedList totalItems={200} onChange={onChangeStub} activePage={3}><div>The list</div></PaginatedList>);

    const pageSizeInput = getByLabelText('Show');

    fireEvent.change(pageSizeInput, { target: { value: 100 } });

    expect(onChangeStub).toHaveBeenCalledTimes(1);
    expect(onChangeStub).toHaveBeenCalledWith(1, 100);
  });
});
