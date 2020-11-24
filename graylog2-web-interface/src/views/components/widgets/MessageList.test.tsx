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
import { mount } from 'wrappedEnzyme';
import * as Immutable from 'immutable';
import { StoreMock as MockStore } from 'helpers/mocking';
import asMock from 'helpers/mocking/AsMock';

import { TIMESTAMP_FIELD, Messages } from 'views/Constants';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldType from 'views/logic/fieldtypes/FieldType';
import { AdditionalContext } from 'views/logic/ActionContext';
import MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import { SelectedFieldsStore } from 'views/stores/SelectedFieldsStore';
import { SearchActions } from 'views/stores/SearchStore';
import { RefreshActions } from 'views/stores/RefreshStore';
import * as messageList from 'views/components/messagelist';
import InputsStore from 'stores/inputs/InputsStore';
import { SearchExecutionResult } from 'views/actions/SearchActions';

import MessageList from './MessageList';
import RenderCompletionCallback from './RenderCompletionCallback';

import InputsActions from '../../../actions/inputs/InputsActions';

const MessageTableEntry = () => (
  <AdditionalContext.Consumer>
    {({ message }) => (
      <tbody>
        <tr><td>{JSON.stringify(message)}</td></tr>
      </tbody>
    )}
  </AdditionalContext.Consumer>
);

const mockEffectiveTimeRange = {
  from: '2019-11-15T14:40:48.666Z',
  to: '2019-11-29T14:40:48.666Z',
  type: 'absolute',
};

jest.mock('views/components/messagelist/MessageTableEntry', () => ({}));

jest.mock('views/stores/ViewStore', () => ({
  ViewStore: MockStore(
    'listen',
    ['getInitialState', () => ({ activeQuery: 'somequery', view: { id: 'someview' } })],
  ),
}));

jest.mock('stores/inputs/InputsStore', () => MockStore('listen', 'getInitialState'));
jest.mock('actions/inputs/InputsActions', () => ({ list: jest.fn(() => Promise.resolve()) }));

jest.mock('views/stores/SelectedFieldsStore', () => ({
  SelectedFieldsStore: MockStore('listen', 'selectedFields'),
}));

jest.mock('views/stores/SearchConfigStore', () => ({
  SearchConfigStore: MockStore('listSearchesClusterConfig', 'configurations', 'listen'),
}));

jest.mock('views/stores/SearchStore', () => ({
  SearchStore: MockStore(
    'listen',
    ['getInitialState', () => ({
      result: {
        results: {
          somequery: {
            searchTypes: {
              'search-type-id': {
                effectiveTimerange: mockEffectiveTimeRange,
              },
            },
          },
        },
      },
    })],
  ),
  SearchActions: {
    reexecuteSearchTypes: jest.fn().mockReturnValue(Promise.resolve({ result: { errors: [] } })),
    execute: { completed: { listen: jest.fn() } },
  },
}));

jest.mock('views/stores/RefreshStore', () => ({
  RefreshActions: {
    disable: jest.fn(),
  },
}));

jest.mock('views/components/messagelist');

describe('MessageList', () => {
  const data = {
    id: 'search-type-id',
    type: 'messages',
    messages: [
      {
        highlight_ranges: {},
        index: 'graylog_42',
        message: {
          _id: 'deadbeef',
          file_name: 'frank.txt',
          timestamp: '2018-09-26T12:42:49.234Z',
        },
      },
    ],
    total: 1,
  };

  beforeEach(() => {
    // eslint-disable-next-line import/namespace
    messageList.MessageTableEntry = MessageTableEntry;
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should render with and without fields', () => {
    const fields = [new FieldTypeMapping('file_name', new FieldType('string', ['full-text-search'], []))];

    SelectedFieldsStore.getInitialState = jest.fn(() => Immutable.Set([TIMESTAMP_FIELD, 'file_name']));
    const config = MessagesWidgetConfig.builder().fields([TIMESTAMP_FIELD, 'file_name']).build();
    const wrapper1 = mount(<MessageList editing
                                        data={data}
                                        config={config}
                                        fields={Immutable.List(fields)}
                                        setLoadingState={() => {}} />);

    expect(wrapper1.find('span[role="presentation"]').length).toBe(2);

    const emptyConfig = MessagesWidgetConfig.builder().fields([]).build();

    SelectedFieldsStore.getInitialState = jest.fn(() => Immutable.Set([]));
    const wrapper2 = mount(<MessageList editing
                                        data={data}
                                        config={emptyConfig}
                                        fields={Immutable.List(fields)}
                                        setLoadingState={() => {}} />);

    expect(wrapper2.find('span[role="presentation"]').length).toBe(0);
  });

  it('provides a message context for each individual entry', () => {
    const fields = [new FieldTypeMapping('file_name', new FieldType('string', ['full-text-search'], []))];
    const config = MessagesWidgetConfig.builder().fields(['file_name']).build();
    const wrapper = mount(<MessageList editing
                                       data={data}
                                       fields={Immutable.List(fields)}
                                       config={config}
                                       setLoadingState={() => {}} />);
    const messageTableEntry = wrapper.find('MessageTableEntry');
    const td = messageTableEntry.find('td').at(0);

    expect(td.props().children).toMatchSnapshot();
  });

  it('renders also when `inputs` is undefined', () => {
    InputsStore.getInitialState = jest.fn(() => ({ inputs: undefined }));
    const config = MessagesWidgetConfig.builder().fields([]).build();

    mount(<MessageList editing
                       data={data}
                       fields={Immutable.List([])}
                       config={config}
                       setLoadingState={() => {}} />);
  });

  it('refreshs Inputs list upon mount', () => {
    const config = MessagesWidgetConfig.builder().fields([]).build();
    const Component = () => (
      <MessageList editing
                   data={data}
                   fields={Immutable.List([])}
                   config={config}
                   setLoadingState={() => {}} />
    );

    mount(<Component />);

    expect(InputsActions.list).toHaveBeenCalled();
  });

  it('reexecute query for search type, when using pagination', () => {
    const searchTypePayload = { [data.id]: { limit: Messages.DEFAULT_LIMIT, offset: Messages.DEFAULT_LIMIT } };
    const config = MessagesWidgetConfig.builder().fields([]).build();
    const secondPageSize = 10;
    const wrapper = mount(<MessageList editing
                                       data={{ ...data, total: Messages.DEFAULT_LIMIT + secondPageSize }}
                                       fields={Immutable.List([])}
                                       config={config}
                                       setLoadingState={() => {}} />);

    wrapper.find('[aria-label="Next"]').simulate('click');

    expect(SearchActions.reexecuteSearchTypes).toHaveBeenCalledWith(searchTypePayload, mockEffectiveTimeRange);
  });

  it('disables refresh actions, when using pagination', () => {
    const config = MessagesWidgetConfig.builder().fields([]).build();
    const secondPageSize = 10;
    const wrapper = mount(<MessageList editing
                                       data={{ ...data, total: Messages.DEFAULT_LIMIT + secondPageSize }}
                                       fields={Immutable.List([])}
                                       config={config}
                                       setLoadingState={() => {}} />);

    wrapper.find('[aria-label="Next"]').simulate('click');

    expect(RefreshActions.disable).toHaveBeenCalledTimes(1);
  });

  it('displays error description, when using pagination throws an error', async () => {
    asMock(SearchActions.reexecuteSearchTypes).mockReturnValue(Promise.resolve({
      result: { errors: [{ description: 'Error description' }] },
    } as SearchExecutionResult));

    const config = MessagesWidgetConfig.builder().fields([]).build();
    const secondPageSize = 10;
    const wrapper = mount(<MessageList editing
                                       data={{ ...data, total: Messages.DEFAULT_LIMIT + secondPageSize }}
                                       fields={Immutable.List([])}
                                       config={config}
                                       setLoadingState={() => {}} />);

    await wrapper.find('[aria-label="Next"]').simulate('click');
    wrapper.update();

    expect(wrapper.find('ErrorWidget').text()).toContain('Error description');
  });

  it('calls render completion callback after first render', () => {
    const config = MessagesWidgetConfig.builder().fields([]).build();
    const Component = () => (
      <MessageList editing
                   data={data}
                   fields={Immutable.List([])}
                   config={config}
                   setLoadingState={() => {}} />
    );

    return new Promise((resolve) => {
      const onRenderComplete = jest.fn(resolve);

      mount((
        <RenderCompletionCallback.Provider value={onRenderComplete}>
          <Component />
        </RenderCompletionCallback.Provider>
      ));
    });
  });
});
