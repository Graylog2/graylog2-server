import React from 'react';
import { mount } from 'enzyme';
import Immutable from 'immutable';
import { CombinedProviderMock, StoreMock, StoreProviderMock } from 'helpers/mocking';

describe('MessageList', () => {
  const StreamsStore = StoreMock('listen', ['listStreams', () => { return { then: jest.fn() }; }], 'availableStreams');
  const ViewStore = StoreMock('listen', ['currentView', () => { return {}; }]);
  const CurrentUserStore = StoreMock('listen', 'get');
  const SelectedFieldsStore = StoreMock('listen', 'selectedFields');
  const storeProviderMock = new StoreProviderMock({
    Streams: StreamsStore,
    CurrentUser: CurrentUserStore,
  });
  const SearchStore = StoreMock('searchSurroundingMessages');
  const combinedProviderMock = new CombinedProviderMock({
    Search: { SearchStore },
    Inputs: { InputsActions: { list: jest.fn() } },
  });
  const SearchConfigStore = StoreMock('listSearchesClusterConfig', 'configurations', 'listen');
  const WidgetActions = StoreMock('updateConfig');

  jest.doMock('injection/CombinedProvider', () => combinedProviderMock);
  jest.doMock('injection/StoreProvider', () => storeProviderMock);
  jest.doMock('legacy/result-histogram', () => 'Histogram');
  jest.doMock('enterprise/stores/SearchConfigStore', () => ({ SearchConfigStore: SearchConfigStore }));
  jest.doMock('enterprise/stores/StreamsStore', () => ({ StreamsStore: StreamsStore }));
  jest.doMock('enterprise/stores/ViewStore', () => ({ ViewStore: ViewStore }));
  jest.doMock('components/search', () => ({ MessageTablePaginator: 'messageTablePaginator' }));
  jest.doMock('enterprise/stores/WidgetStore', () => WidgetActions);
  jest.doMock('enterprise/stores/SelectedFieldsStore', () => ({ SelectedFieldsStore: SelectedFieldsStore }));

  it('should render with and without fields', () => {
    const fields = [{
      value: {
        name: 'file_name',
        type: {
          value: {
            type: 'string',
            properties: [
              'full-text-search',
            ],
            indexNames: [],
          },
        },
      },
    }];
    const data = {
      id: '6ec30961-2519-45f5-80b6-849e3deb1c32',
      type: 'messages',
      messages: [
        {
          highlight_ranges: {},
          message: {
            file_name: 'frank.txt',
          },
        }],
    };
    const MessageList = require('./MessageList').default;
    const wrapper1 = mount(<MessageList editing
                                       data={data}
                                       fields={Immutable.List(fields)}
                                       config={{ fields: ['file_name'] }} />);

    expect(wrapper1.find('span[role="presentation"]').length).toBe(2);

    const wrapper2 = mount(<MessageList editing
                                        data={data}
                                        fields={Immutable.List(fields)}
                                        config={{ fields: [] }} />);
    expect(wrapper2.find('span[role="presentation"]').length).toBe(1);
  });
});