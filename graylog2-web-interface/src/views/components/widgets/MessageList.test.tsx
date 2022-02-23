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
import { mount } from 'wrappedEnzyme';
import * as Immutable from 'immutable';

import { StoreMock as MockStore } from 'helpers/mocking';
import asMock from 'helpers/mocking/AsMock';
import { TIMESTAMP_FIELD, Messages } from 'views/Constants';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldType from 'views/logic/fieldtypes/FieldType';
import { AdditionalContext } from 'views/logic/ActionContext';
import MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import { SearchActions } from 'views/stores/SearchStore';
import { RefreshActions } from 'views/stores/RefreshStore';
import * as messageList from 'views/components/messagelist';
import { InputsActions, InputsStore } from 'stores/inputs/InputsStore';
import type { SearchExecutionResult } from 'views/actions/SearchActions';
import type { FieldTypeMappingsList } from 'views/logic/fieldtypes/types';
import CancellablePromise from 'logic/rest/CancellablePromise';

import type { MessageListResult } from './MessageList';
import MessageList from './MessageList';
import type { TRenderCompletionCallback } from './RenderCompletionCallback';
import RenderCompletionCallback from './RenderCompletionCallback';

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
    ['getInitialState', () => ({ activeQuery: 'somequery', view: { id: 'someview' } })],
  ),
}));

jest.mock('stores/inputs/InputsStore', () => ({
  InputsStore: MockStore(),
  InputsActions: { list: jest.fn(() => Promise.resolve()) },
}));

jest.mock('views/stores/SearchConfigStore', () => ({
  SearchConfigStore: MockStore('listSearchesClusterConfig', 'configurations'),
}));

const mockReexecuteResult = CancellablePromise.of(Promise.resolve({ result: { errors: [] } }));

jest.mock('views/stores/SearchStore', () => ({
  SearchStore: MockStore(
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
    reexecuteSearchTypes: jest.fn(() => mockReexecuteResult),
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
  const data: MessageListResult = {
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
    // @ts-ignore
    messageList.MessageTableEntry = MessageTableEntry; // eslint-disable-line no-import-assign
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  const SimpleMessageList = (props: {
    config: MessagesWidgetConfig,
    data: MessageListResult,
    fields: FieldTypeMappingsList,
    setLoadingState: () => void,
  }) => (
    <MessageList title="Message List"
                 editing={false}
                 filter=""
                 type="messages"
                 id="message-list"
                 queryId="deadbeef"
                 toggleEdit={() => {}}
                 {...props} />
  );

  it('should render with and without fields', () => {
    const fields = [new FieldTypeMapping('file_name', new FieldType('string', ['full-text-search'], []))];

    const config = MessagesWidgetConfig.builder().fields([TIMESTAMP_FIELD, 'file_name']).build();
    const wrapper1 = mount(
      <SimpleMessageList data={data}
                         config={config}
                         fields={Immutable.List(fields)}
                         setLoadingState={() => {}} />,
    );

    expect(wrapper1.find('span[role="presentation"]').length).toBe(2);

    const emptyConfig = MessagesWidgetConfig.builder().fields([]).build();

    const wrapper2 = mount(
      <SimpleMessageList data={data}
                         config={emptyConfig}
                         fields={Immutable.List(fields)}
                         setLoadingState={() => {}} />,
    );

    expect(wrapper2.find('span[role="presentation"]').length).toBe(0);
  });

  it('provides a message context for each individual entry', () => {
    const fields = [new FieldTypeMapping('file_name', new FieldType('string', ['full-text-search'], []))];
    const config = MessagesWidgetConfig.builder().fields(['file_name']).build();
    const wrapper = mount(
      <SimpleMessageList data={data}
                         fields={Immutable.List(fields)}
                         config={config}
                         setLoadingState={() => {}} />,
    );
    const messageTableEntry = wrapper.find('MessageTableEntry');
    const td = messageTableEntry.find('td').at(0);

    expect(td.props().children).toMatchSnapshot();
  });

  // eslint-disable-next-line jest/expect-expect
  it('renders also when `inputs` is undefined', () => {
    InputsStore.getInitialState = jest.fn(() => ({ inputs: undefined }));
    const config = MessagesWidgetConfig.builder().fields([]).build();

    mount(<SimpleMessageList data={data}
                             fields={Immutable.List([])}
                             config={config}
                             setLoadingState={() => {}} />);
  });

  it('refreshs Inputs list upon mount', () => {
    const config = MessagesWidgetConfig.builder().fields([]).build();
    const Component = () => (
      <SimpleMessageList data={data}
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
    const wrapper = mount(<SimpleMessageList data={{ ...data, total: Messages.DEFAULT_LIMIT + secondPageSize }}
                                             fields={Immutable.List([])}
                                             config={config}
                                             setLoadingState={() => {}} />);

    wrapper.find('[aria-label="Next"]').simulate('click');

    expect(SearchActions.reexecuteSearchTypes).toHaveBeenCalledWith(searchTypePayload, mockEffectiveTimeRange);
  });

  it('disables refresh actions, when using pagination', () => {
    const config = MessagesWidgetConfig.builder().fields([]).build();
    const secondPageSize = 10;
    const wrapper = mount(<SimpleMessageList data={{ ...data, total: Messages.DEFAULT_LIMIT + secondPageSize }}
                                             fields={Immutable.List([])}
                                             config={config}
                                             setLoadingState={() => {}} />);

    wrapper.find('[aria-label="Next"]').simulate('click');

    expect(RefreshActions.disable).toHaveBeenCalledTimes(1);
  });

  it('displays error description, when using pagination throws an error', async () => {
    asMock(SearchActions.reexecuteSearchTypes).mockReturnValue(CancellablePromise.of(Promise.resolve({
      result: { errors: [{ description: 'Error description' }] },
    } as SearchExecutionResult)));

    const config = MessagesWidgetConfig.builder().fields([]).build();
    const secondPageSize = 10;
    const wrapper = mount(<SimpleMessageList data={{ ...data, total: Messages.DEFAULT_LIMIT + secondPageSize }}
                                             fields={Immutable.List([])}
                                             config={config}
                                             setLoadingState={() => {}} />);

    await wrapper.find('[aria-label="Next"]').simulate('click');
    wrapper.update();

    expect(wrapper.find('ErrorWidget').text()).toContain('Error description');
  });

  // eslint-disable-next-line jest/expect-expect
  it('calls render completion callback after first render', () => {
    const config = MessagesWidgetConfig.builder().fields([]).build();
    const Component = () => (
      <SimpleMessageList data={data}
                         fields={Immutable.List([])}
                         config={config}
                         setLoadingState={() => {}} />
    );

    return new Promise<void>((resolve) => {
      const onRenderComplete: TRenderCompletionCallback = jest.fn(() => resolve());

      mount((
        <RenderCompletionCallback.Provider value={onRenderComplete}>
          <Component />
        </RenderCompletionCallback.Provider>
      ));
    });
  });
});
