import React from 'react';
import { mount } from 'enzyme';
import Immutable from 'immutable';
import { CombinedProviderMock, StoreMock, StoreProviderMock } from 'helpers/mocking';
import FieldTypeMapping from '../../logic/fieldtypes/FieldTypeMapping';
import FieldType from '../../logic/fieldtypes/FieldType';

describe('MessageList', () => {
  const StreamsStore = StoreMock('listen', ['listStreams', () => ({ then: jest.fn() })], 'availableStreams');
  const ViewStore = StoreMock('listen', ['getInitialState', () => ({ activeQuery: 'somequery', view: { id: 'someview' } })]);
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
  jest.doMock('components/search', () => ({ MessageTablePaginator: 'message-table-paginator' }));
  jest.doMock('enterprise/stores/WidgetStore', () => WidgetActions);
  jest.doMock('enterprise/stores/SelectedFieldsStore', () => ({ SelectedFieldsStore: SelectedFieldsStore }));

  it('should render with and without fields', () => {
    const fields = [new FieldTypeMapping('file_name', new FieldType('string', ['full-text-search'], []))];
    const data = {
      id: '6ec30961-2519-45f5-80b6-849e3deb1c32',
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
        }],
    };
    // eslint-disable-next-line global-require
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
