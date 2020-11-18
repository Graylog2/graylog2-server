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
// @flow strict
import * as React from 'react';
import { fireEvent, render, waitFor } from 'wrappedTestingLibrary';
import { act } from 'react-dom/test-utils';
import { StoreMock as MockStore } from 'helpers/mocking';

import { GlobalOverrideActions } from 'views/stores/GlobalOverrideStore';

import DashboardSearchBar from './DashboardSearchBar';

jest.mock('views/components/ViewActionsMenu', () => () => <span>View Actions</span>);

jest.mock('views/stores/GlobalOverrideStore', () => ({
  GlobalOverrideActions: {
    set: jest.fn(() => Promise.resolve()),
  },
  GlobalOverrideStore: MockStore(),
}));

const config = {
  analysis_disabled_fields: ['full_message', 'message'],
  query_time_range_limit: 'PT0S',
  relative_timerange_options: { PT0S: 'Search in all messages', P5m: 'Search in last five minutes' },
  surrounding_filter_fields: ['file', 'source', 'gl2_source_input', 'source_file'],
  surrounding_timerange_options: { PT1S: 'One second', PT2S: 'Two seconds' },
};

describe('DashboardSearchBar', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  const onExecute = jest.fn();

  it('defaults to no override being selected', () => {
    const { container, getByTitle } = render(<DashboardSearchBar onExecute={onExecute} config={config} />);

    expect(container).not.toBeNull();
    expect(getByTitle('There is no override for the timerange currently selected')).toBeVisible();
  });

  it('allows selecting relative time range', async () => {
    const { getByText, queryByText, getByTitle } = render(<DashboardSearchBar onExecute={onExecute} config={config} />);

    expect(queryByText('Search in last five minutes')).toBeNull();

    const relativeTimerange = getByText('Relative');

    fireEvent.click(relativeTimerange);

    const searchButton = getByTitle(/Perform search/);

    fireEvent.click(searchButton);

    expect(getByText('Search in last five minutes')).toBeVisible();

    await waitFor(() => expect(GlobalOverrideActions.set).toHaveBeenCalledWith({ type: 'relative', range: 300 }, ''));

    expect(onExecute).toHaveBeenCalled();
  });

  it('allows selecting absolute time range', async () => {
    const { getByText, getAllByPlaceholderText, queryByPlaceholderText, getByTitle } = render(<DashboardSearchBar onExecute={onExecute} config={config} />);

    expect(queryByPlaceholderText('YYYY-MM-DD HH:mm:ss')).toBeNull();

    const absoluteTimerange = getByText('Absolute');

    fireEvent.click(absoluteTimerange);

    getAllByPlaceholderText('YYYY-MM-DD HH:mm:ss').map((input) => expect(input).toBeVisible());

    const searchButton = getByTitle(/Perform search/);

    fireEvent.click(searchButton);

    await waitFor(() => expect(GlobalOverrideActions.set).toHaveBeenCalledWith(expect.objectContaining({
      type: 'absolute',
      from: expect.anything(),
      to: expect.anything(),
    }), ''));

    expect(onExecute).toHaveBeenCalled();
  });

  it('allows selecting keyword time range', async () => {
    const { getByText, getByPlaceholderText, queryByPlaceholderText, getByTitle } = render(<DashboardSearchBar onExecute={onExecute} config={config} />);

    expect(queryByPlaceholderText('Last week')).toBeNull();

    const keywordTimerange = getByText('Keyword');

    await act(async () => {
      fireEvent.click(keywordTimerange);
    });

    expect(getByPlaceholderText('Last week')).toBeVisible();

    const searchButton = getByTitle(/Perform search/);

    fireEvent.click(searchButton);

    await waitFor(() => expect(GlobalOverrideActions.set).toHaveBeenCalledWith({ type: 'keyword', keyword: 'Last five minutes' }, ''));

    expect(onExecute).toHaveBeenCalled();
  });

  it('allows resetting the timerange override', async () => {
    const { getByText, getByTitle } = render(<DashboardSearchBar onExecute={onExecute} config={config} />);
    const relativeTimerange = getByText('Relative');

    fireEvent.click(relativeTimerange);

    const disableOverride = getByText('No Override');

    fireEvent.click(disableOverride);

    const searchButton = getByTitle(/Perform search/);

    fireEvent.click(searchButton);

    await waitFor(() => expect(GlobalOverrideActions.set).toHaveBeenCalledWith(undefined, ''));

    expect(onExecute).toHaveBeenCalled();
  });
});
