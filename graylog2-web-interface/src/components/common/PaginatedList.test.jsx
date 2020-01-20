import React from 'react';
import { render, cleanup } from 'wrappedTestingLibrary';

import InteractiveContext from 'views/components/contexts/InteractiveContext';
import PaginatedList from './PaginatedList';

describe('PaginatedList', () => {
  afterEach(cleanup);

  it('should display Pagination', () => {
    const { getByText } = render(<PaginatedList totalItems={100} onChange={() => {}} pageSize={10}>List</PaginatedList>);
    expect(getByText('1')).not.toBeNull();
  });
  it('should not display Pagination, when context is not interactive', () => {
    const { queryByText } = render(
      <InteractiveContext.Provider value={false}>
        <PaginatedList totalItems={100} onChange={() => {}} pageSize={10}>List</PaginatedList>,
      </InteractiveContext.Provider>,
    );
    expect(queryByText('1')).toBeNull();
  });
});
