// @flow strict
import React from 'react';
import { render, cleanup, fireEvent } from 'wrappedTestingLibrary';

import InteractiveContext from 'views/components/contexts/InteractiveContext';
import PaginatedList from './PaginatedList';

describe('PaginatedList', () => {
  afterEach(cleanup);

  it('should display Pagination', () => {
    const { getByText } = render(<PaginatedList totalItems={100} onChange={() => {}}>The list</PaginatedList>);
    expect(getByText('The list')).not.toBeNull();
    expect(getByText('1')).not.toBeNull();
  });

  it('should not display Pagination, when context is not interactive', () => {
    const { queryByText } = render(
      <InteractiveContext.Provider value={false}>
        <PaginatedList totalItems={100} onChange={() => {}}>The list</PaginatedList>,
      </InteractiveContext.Provider>,
    );
    expect(queryByText('The list')).toBeNull();
    expect(queryByText('1')).toBeNull();
  });

  it('should reset current page on page size change', () => {
    const onChangeStub = jest.fn();
    const { getByLabelText } = render(<PaginatedList totalItems={200} onChange={onChangeStub} activePage={3}>The list</PaginatedList>);

    const pageSizeInput = getByLabelText('Show:');
    fireEvent.change(pageSizeInput, { target: { value: 100 } });

    expect(onChangeStub).toHaveBeenCalledTimes(1);
    expect(onChangeStub).toHaveBeenCalledWith(1, 100);
  });
});
