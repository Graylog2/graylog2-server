// @flow strict
import React from 'react';
import { mount } from 'enzyme';
import Immutable from 'immutable';
// $FlowFixMe: imports from core need to be fixed in flow
import { CombinedProviderMock, StoreMock, StoreProviderMock } from 'helpers/mocking';

import FieldTypeMapping from 'enterprise/logic/fieldtypes/FieldTypeMapping';
import FieldType from 'enterprise/logic/fieldtypes/FieldType';
import { AdditionalContext } from 'enterprise/logic/ActionContext';
import MessagesWidgetConfig from 'enterprise/logic/widgets/MessagesWidgetConfig';
import { TIMESTAMP_FIELD } from 'enterprise/Constants';

jest.mock('enterprise/components/messagelist/MessageTableEntry', () => ({}));

describe('MessageList', () => {
  const ViewStore = StoreMock('listen', ['getInitialState', () => ({ activeQuery: 'somequery', view: { id: 'someview' } })]);
  const CurrentUserStore = StoreMock('listen', 'get');
  const SelectedFieldsStore = StoreMock('listen', 'selectedFields');
  const storeProviderMock = new StoreProviderMock({
    CurrentUser: CurrentUserStore,
  });
  const SearchStore = StoreMock('searchSurroundingMessages');

  const InputsStore = StoreMock('listen', 'getInitialState');
  const combinedProviderMock = new CombinedProviderMock({
    Search: { SearchStore },
    Inputs: { InputsActions: { list: jest.fn() }, InputsStore },
  });
  const SearchConfigStore = StoreMock('listSearchesClusterConfig', 'configurations', 'listen');
  const WidgetActions = StoreMock('updateConfig');

  const MessageTableEntry = () => (
    <AdditionalContext.Consumer>
      {({ message }) => (
        <tbody>
          <tr><td>{JSON.stringify(message)}</td></tr>
        </tbody>
      )}
    </AdditionalContext.Consumer>
  );

  jest.doMock('injection/CombinedProvider', () => combinedProviderMock);
  jest.doMock('injection/StoreProvider', () => storeProviderMock);
  jest.doMock('legacy/result-histogram', () => 'Histogram');
  jest.doMock('enterprise/stores/SearchConfigStore', () => ({ SearchConfigStore: SearchConfigStore }));
  jest.doMock('enterprise/stores/ViewStore', () => ({ ViewStore: ViewStore }));
  jest.doMock('components/search/MessageTablePaginator', () => 'message-table-paginator');
  jest.doMock('enterprise/stores/WidgetStore', () => WidgetActions);
  jest.doMock('enterprise/stores/SelectedFieldsStore', () => ({ SelectedFieldsStore: SelectedFieldsStore }));
  jest.doMock('enterprise/components/messagelist', () => ({ MessageTableEntry }));

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

  let MessageList;
  beforeEach(() => {
    // eslint-disable-next-line global-require
    MessageList = require('./MessageList').default;
  });

  it('should render with and without fields', () => {
    const fields = [new FieldTypeMapping('file_name', new FieldType('string', ['full-text-search'], []))];
    SelectedFieldsStore.getInitialState = jest.fn(() => Immutable.Set([TIMESTAMP_FIELD, 'file_name']));
    const wrapper1 = mount(<MessageList editing
                                        data={data}
                                        fields={Immutable.List(fields)} />);

    expect(wrapper1.find('span[role="presentation"]').length).toBe(2);

    SelectedFieldsStore.getInitialState = jest.fn(() => Immutable.Set([]));
    const wrapper2 = mount(<MessageList editing
                                        data={data}
                                        fields={Immutable.List(fields)} />);
    expect(wrapper2.find('span[role="presentation"]').length).toBe(0);
  });
  it('provides a message context for each individual entry', () => {
    const fields = [new FieldTypeMapping('file_name', new FieldType('string', ['full-text-search'], []))];
    const config = MessagesWidgetConfig.builder().fields(['file_name']).build();
    const wrapper = mount(<MessageList editing
                                       data={data}
                                       fields={Immutable.List(fields)}
                                       config={config} />);
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
                       config={config} />);
  });
});
