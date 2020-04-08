// @flow strict
import * as React from 'react';
import { cleanup, fireEvent, render } from 'wrappedTestingLibrary';
import { act } from 'react-dom/test-utils';

import DashboardSearchBar from './DashboardSearchBar';

jest.mock('views/components/ViewActionsMenu', () => () => <span>View Actions</span>);

const config = {
  analysis_disabled_fields: ['full_message', 'message'],
  query_time_range_limit: 'PT0S',
  relative_timerange_options: { PT0S: 'Search in all messages', P1D: 'Search in last day' },
  surrounding_filter_fields: ['file', 'source', 'gl2_source_input', 'source_file'],
  surrounding_timerange_options: { PT1S: 'One second', PT2S: 'Two seconds' },
};

describe('DashboardSearchBar', () => {
  afterEach(cleanup);
  const onExecute = jest.fn();
  it('defaults to no override being selected', () => {
    const { container, getByTitle } = render(<DashboardSearchBar onExecute={onExecute} config={config} />);
    expect(container).not.toBeNull();
    expect(getByTitle('There is no override for the timerange currently selected')).toBeVisible();
  });

  it('allows selecting relative time range', () => {
    const { getByText, queryByText } = render(<DashboardSearchBar onExecute={onExecute} config={config} />);
    expect(queryByText('Search in last day')).toBeNull();
    const relativeTimerange = getByText('Relative');

    fireEvent.click(relativeTimerange);

    expect(getByText('Search in last day')).toBeVisible();
  });

  it('allows selecting absolute time range', () => {
    const { getByText, getAllByPlaceholderText, queryByPlaceholderText } = render(<DashboardSearchBar onExecute={onExecute} config={config} />);
    expect(queryByPlaceholderText('YYYY-MM-DD HH:mm:ss')).toBeNull();
    const absoluteTimerange = getByText('Absolute');

    fireEvent.click(absoluteTimerange);

    getAllByPlaceholderText('YYYY-MM-DD HH:mm:ss').map((input) => expect(input).toBeVisible());
  });

  it('allows selecting keyword time range', async () => {
    const { getByText, getByPlaceholderText, queryByPlaceholderText } = render(<DashboardSearchBar onExecute={onExecute} config={config} />);
    expect(queryByPlaceholderText('Last week')).toBeNull();
    const keywordTimerange = getByText('Keyword');

    await act(async () => {
      fireEvent.click(keywordTimerange);
    });

    expect(getByPlaceholderText('Last week')).toBeVisible();
  });
});
