// @flow strict
import React from 'react';
import { cleanup, render } from 'wrappedTestingLibrary';
import View from 'views/logic/views/View';
import ViewTypeLabel from './ViewTypeLabel';

describe('ViewTypeLabel', () => {
  afterEach(cleanup);
  it('should create correct label for view type search', () => {
    const { getByText } = render(<ViewTypeLabel type={View.Type.Search} />);
    expect(getByText('search')).not.toBe(null);
  });
  afterEach(cleanup);
  it('should create correct label for view type dasboard', () => {
    const { getByText } = render(<ViewTypeLabel type={View.Type.Dashboard} />);
    expect(getByText('dashboard')).not.toBe(null);
  });
  it('should create capitalized label', () => {
    const { getByText } = render(<ViewTypeLabel type={View.Type.Search} capitalize />);
    expect(getByText('Search')).not.toBe(null);
  });
});
