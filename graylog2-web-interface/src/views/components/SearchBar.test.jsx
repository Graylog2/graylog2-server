// @flow strict
import * as React from 'react';
import { mountWithTheme as mount } from 'theme/enzymeWithTheme';
import * as Immutable from 'immutable';

import { StoreMock as MockStore } from 'helpers/mocking';
import { QueriesActions } from 'views/stores/QueriesStore';
import mockAction from 'helpers/mocking/MockAction';
import SearchBar from './SearchBar';

jest.mock('stores/sessions/SessionStore', () => MockStore(['isLoggedIn', () => { return true; }], 'getSessionId'));
jest.mock('stores/users/CurrentUserStore', () => MockStore('listen', 'get'));
jest.mock('actions/sessions/SessionActions', () => ({
  logout: {
    completed: {
      listen: jest.fn(),
    },
  },
}));
jest.mock('stores/streams/StreamsStore', () => MockStore(
  'listen',
  ['listStreams', () => ({ then: jest.fn() })],
  'availableStreams',
));
jest.mock('views/stores/ViewManagementStore', () => ({
  ViewManagementActions: {
    update: {
      completed: {
        listen: jest.fn(),
      },
    },
    create: {
      completed: {
        listen: jest.fn(),
      },
    },
  },
}));
jest.mock('views/components/searchbar/QueryInput', () => 'query-input');

describe('SearchBar', () => {
  const config = {
    analysis_disabled_fields: ['full_message', 'message'],
    query_time_range_limit: 'PT0S',
    relative_timerange_options: { PT0S: 'Search in all messages', PT1D: 'Search in last day' },
    surrounding_filter_fields: ['file', 'source', 'gl2_source_input', 'source_file'],
    surrounding_timerange_options: { PT1S: 'One second', PT2S: 'Two seconds' },
  };
  const currentQuery = {
    timerange: { type: 'relative', range: 300 },
    query: { type: 'elasticsearch', query_string: '*' },
    id: '34efae1e-e78e-48ab-ab3f-e83c8611a683',
  };

  it('should render the SearchBar', () => {
    // Empty currentQuery
    const wrapper = mount(<SearchBar config={config} onExecute={() => {}} />);
    expect(wrapper.find('span').text()).toBe(' Loading...');
    expect(wrapper.find('option').length).toBe(0);

    // Set new currentQuery
    wrapper.setState({ currentQuery });
    expect(wrapper.find('option').at(0).text()).toBe('Search in last day');
    expect(wrapper.find('option').at(1).text()).toBe('Search in all messages');
    expect(wrapper.find('option').at(2).length).toBe(0);
  });

  it('should execute a search', () => {
    const executeFn = jest.fn();
    const wrapper = mount(<SearchBar config={config} onExecute={executeFn} />);
    wrapper.setState({ currentQuery });
    wrapper.find('form').simulate('submit');
    expect(executeFn).toHaveBeenCalledTimes(1);
  });

  it('changing the time range type executes a new search', () => {
    const executeFn = jest.fn();
    const wrapper = mount(<SearchBar config={config} onExecute={executeFn} />);
    wrapper.setState({ currentQuery });

    const timeRangeTypeSelector = wrapper.find('TimeRangeTypeSelector');
    const { onSelect } = timeRangeTypeSelector.at(0).props();
    QueriesActions.rangeType = mockAction(() => Promise.resolve(new Immutable.OrderedMap()));

    return onSelect('absolute').then(() => {
      expect(executeFn).toHaveBeenCalled();
    });
  });

  it('changing the time range value executes a new search', () => {
    const executeFn = jest.fn();
    const wrapper = mount(<SearchBar config={config} onExecute={executeFn} />);
    wrapper.setState({ currentQuery });

    const timeRangeInput = wrapper.find('TimeRangeInput');
    const { onChange } = timeRangeInput.at(0).props();
    QueriesActions.rangeParams = mockAction(jest.fn(() => Promise.resolve(new Immutable.OrderedMap())));

    return onChange({ range: 300 }).then(() => {
      expect(executeFn).toHaveBeenCalled();
    });
  });
});
