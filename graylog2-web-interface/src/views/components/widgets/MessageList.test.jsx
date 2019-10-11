// @flow strict
import * as React from 'react';
import { mount } from 'enzyme';
import * as Immutable from 'immutable';
import { StoreMock as MockStore } from 'helpers/mocking';

import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldType from 'views/logic/fieldtypes/FieldType';
import { AdditionalContext } from 'views/logic/ActionContext';
import MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import { TIMESTAMP_FIELD } from 'views/Constants';
import { SelectedFieldsStore } from 'views/stores/SelectedFieldsStore';
import InputsStore from 'stores/inputs/InputsStore';
import * as messageList from 'views/components/messagelist';
import MessageList from './MessageList';

const MessageTableEntry = () => (
  <AdditionalContext.Consumer>
    {({ message }) => (
      <tbody>
        <tr><td>{JSON.stringify(message)}</td></tr>
      </tbody>
    )}
  </AdditionalContext.Consumer>
);

jest.mock('views/components/messagelist/MessageTableEntry', () => ({}));
jest.mock('stores/search/SearchStore', () => MockStore('searchSurroundingMessages'));
jest.mock('views/stores/ViewStore', () => ({
  ViewStore: MockStore(
    'listen',
    ['getInitialState', () => ({ activeQuery: 'somequery', view: { id: 'someview' } })],
  ),
}));
jest.mock('stores/inputs/InputsStore', () => ({
  InputsStore: MockStore('listen', 'getInitialState'),
  InputsActions: { list: jest.fn() },
}));
jest.mock('stores/users/CurrentUserStore', () => MockStore('listen', 'get'));
jest.mock('views/stores/SelectedFieldsStore', () => ({
  SelectedFieldsStore: MockStore('listen', 'selectedFields'),
}));
jest.mock('views/stores/SearchConfigStore', () => ({
  SearchConfigStore: MockStore('listSearchesClusterConfig', 'configurations', 'listen'),
}));
jest.mock('legacy/result-histogram', () => 'Histogram');
jest.mock('components/search/MessageTablePaginator', () => 'message-table-paginator');
jest.mock('views/components/messagelist');

describe('MessageList', () => {
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
  beforeEach(() => {
    // eslint-disable-next-line import/namespace
    messageList.MessageTableEntry = MessageTableEntry;
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
