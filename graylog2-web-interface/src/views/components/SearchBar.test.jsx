// @flow strict
import * as React from 'react';
import { asElement, cleanup, fireEvent, render, wait } from 'wrappedTestingLibrary';
import { act } from 'react-dom/test-utils';

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

  afterEach(cleanup);

  it('should render the SearchBar', () => {
    const { getByText } = render(<SearchBar config={config} onExecute={() => {}} />);

    expect(getByText('Search in last day')).not.toBeNull();
    expect(getByText('Search in all messages')).not.toBeNull();
  });

  it('should update query when search is performed', async () => {
    const { getByTitle } = render(<SearchBar config={config} />);

    const searchButton = getByTitle('Perform search');
    fireEvent.click(searchButton);

    const queryId = '34efae1e-e78e-48ab-ab3f-e83c8611a683';

    await wait(() => expect(QueriesActions.update).toHaveBeenCalledWith(queryId, expect.objectContaining({ id: queryId })));
  });

  it('changing the time range type does not execute a new search', async () => {
    const onSubmit = jest.fn(() => Promise.resolve());
    const { getByText } = render(<SearchBar config={config} onSubmit={onSubmit} />);
    const absoluteTimeRange = getByText('Absolute');

    fireEvent.click(absoluteTimeRange);

    await wait(() => expect(onSubmit).not.toHaveBeenCalled());
  });

  const selectOption = (option) => {
    const { parentNode, value } = asElement(option, HTMLOptionElement);
    const input = asElement(parentNode, HTMLSelectElement);
    const { name } = input;
    fireEvent.change(input, { target: { name, value: String(value) } });
  };

  it('changing the relative time range value does not execute a new search', async () => {
    const onSubmit = jest.fn();
    const { getByText } = render(<SearchBar config={config} onSubmit={onSubmit} />);

    const lastDay = getByText('Search in last day');

    await act(async () => selectOption(lastDay));

    expect(onSubmit).not.toHaveBeenCalled();
  });
});
