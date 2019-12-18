// @flow strict
import * as React from 'react';
import { mount } from 'wrappedEnzyme';
import * as Immutable from 'immutable';

import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldType from 'views/logic/fieldtypes/FieldType';
import MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import MessageTable from './MessageTable';

const messages = [
  {
    highlight_ranges: {},
    index: 'graylog_42',
    message: {
      _id: 'message-id-1',
      file_name: 'frank.txt',
      timestamp: '2018-09-26T12:42:49.234Z',
    },
  },

];
const fields = [new FieldTypeMapping('file_name', new FieldType('string', ['full-text-search'], []))];
const config = MessagesWidgetConfig.builder().fields(['file_name']).build();
const activeQueryId = 'some-query-id';


describe('MessageTable', () => {
  it('lists provided field in table head', () => {
    const wrapper = mount(<MessageTable messages={messages}
                                        activeQueryId={activeQueryId}
                                        fields={Immutable.List(fields)}
                                        selectedFields={{}}
                                        config={config} />);
    const th = wrapper.find('th').at(0);
    expect(th.text()).toContain('file_name');
  });

  it('renders a table entry for messages', () => {
    const wrapper = mount(<MessageTable messages={messages}
                                        activeQueryId={activeQueryId}
                                        fields={Immutable.List(fields)}
                                        selectedFields={{}}
                                        config={config} />);
    const messageTableEntry = wrapper.find('MessageTableEntry');
    const td = messageTableEntry.find('td').at(0);
    expect(td.text()).toContain('frank.txt');
  });
});
