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
import * as React from 'react';
import { fireEvent, render, screen, waitFor } from 'wrappedTestingLibrary';
import { StoreMock as MockStore } from 'helpers/mocking';
import mockAction from 'helpers/mocking/MockAction';

import { QueriesActions } from 'views/stores/QueriesStore';
// eslint-disable-next-line import/no-named-default
import { default as MockQuery } from 'views/logic/queries/Query';

import SearchBar from './SearchBar';

const mockCurrentUser = { currentUser: { fullname: 'Ada Lovelace', username: 'ada' } };

jest.mock('stores/users/CurrentUserStore', () => MockStore(
  ['get', () => mockCurrentUser],
  ['getInitialState', () => mockCurrentUser],
));

jest.mock('stores/streams/StreamsStore', () => MockStore(
  'listen',
  ['listStreams', () => ({ then: jest.fn() })],
  'availableStreams',
));

jest.mock('views/components/searchbar/QueryInput', () => 'query-input');
jest.mock('views/components/searchbar/saved-search/SavedSearchControls', () => 'saved-search-controls');

jest.mock('views/stores/CurrentQueryStore', () => ({
  CurrentQueryStore: MockStore(['getInitialState', () => MockQuery.builder()
    .timerange({ type: 'relative', range: 300 })
    .query({ type: 'elasticsearch', query_string: '*' })
    .id('34efae1e-e78e-48ab-ab3f-e83c8611a683')
    .build()]),
}));

describe('SearchBar', () => {
  const config = {
    analysis_disabled_fields: ['full_message', 'message'],
    query_time_range_limit: 'PT0S',
    relative_timerange_options: { PT0S: 'Search in all messages', P1D: 'Search in last day' },
    surrounding_filter_fields: ['file', 'source', 'gl2_source_input', 'source_file'],
    surrounding_timerange_options: { PT1S: 'One second', PT2S: 'Two seconds' },
  };

  beforeEach(() => {
    QueriesActions.update = mockAction(jest.fn());
  });

  it('should render the SearchBar', () => {
    render(<SearchBar config={config} />);

    const timeRangeButton = screen.getByLabelText('Open Time Range Selector');
    const timeRangeDisplay = screen.getByLabelText('Search Time Range');
    const streamsFilter = screen.getByTestId('streams-filter');
    const liveUpdate = screen.getByLabelText('Refresh Search Controls');
    const searchButton = screen.getByTitle('Perform search');
    const metaButtons = screen.getByLabelText('Search Meta Buttons');

    expect(timeRangeButton).not.toBeNull();
    expect(timeRangeDisplay).not.toBeNull();
    expect(streamsFilter).not.toBeNull();
    expect(liveUpdate).not.toBeNull();
    expect(searchButton).not.toBeNull();
    expect(metaButtons).not.toBeNull();
  });

  it('should update query when search is performed', async () => {
    render(<SearchBar config={config} />);

    const searchButton = screen.getByTitle('Perform search');

    fireEvent.click(searchButton);

    const queryId = '34efae1e-e78e-48ab-ab3f-e83c8611a683';

    await waitFor(() => expect(QueriesActions.update).toHaveBeenCalledWith(queryId, expect.objectContaining({ id: queryId })));
  });

  it('date exceeding limitDuration should render with error Icon & search button disabled', () => {
    render(<SearchBar config={{ ...config, query_time_range_limit: 'PT1M' }} />);

    const timeRangeButton = screen.getByLabelText('Open Time Range Selector');
    const searchButton = screen.getByTitle('Perform search');

    expect(searchButton).toHaveAttribute('disabled');
    expect(timeRangeButton.firstChild).toHaveClass('fa-exclamation-triangle');
  });
});
