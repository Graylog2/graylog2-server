// @flow strict
import * as React from 'react';
import { mount } from 'wrappedEnzyme';
import * as Immutable from 'immutable';

import suppressConsole from 'helpers/suppressConsole';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldType from 'views/logic/fieldtypes/FieldType';
import MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import MessageTable from './MessageTable';
import HighlightMessageContext from '../contexts/HighlightMessageContext';

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
    const wrapper = mount(<MessageTable activeQueryId={activeQueryId}
                                        config={config}
                                        fields={Immutable.List(fields)}
                                        messages={messages}
                                        onSortChange={() => Promise.resolve()}
                                        selectedFields={{}}
                                        setLoadingState={() => {}} />);
    const th = wrapper.find('th').at(0);
    expect(th.text()).toContain('file_name');
  });

  it('renders a table entry for messages', () => {
    const wrapper = mount(<MessageTable activeQueryId={activeQueryId}
                                        config={config}
                                        fields={Immutable.List(fields)}
                                        onSortChange={() => Promise.resolve()}
                                        selectedFields={{}}
                                        setLoadingState={() => {}}
                                        messages={messages} />);
    const messageTableEntry = wrapper.find('MessageTableEntry');
    const td = messageTableEntry.find('td').at(0);
    expect(td.text()).toContain('frank.txt');
  });

  it('renders a table entry for messages, even if fields are `undefined`', () => {
    // Suppressing console to disable props warning because of `fields` being `undefined`.
    suppressConsole(() => {
      const wrapper = mount(<MessageTable activeQueryId={activeQueryId}
                                          config={config}
                                          // $FlowFixMe: violating contract on purpose
                                          fields={undefined}
                                          onSortChange={() => Promise.resolve()}
                                          selectedFields={{}}
                                          setLoadingState={() => {}}
                                          messages={messages} />);
      const messageTableEntry = wrapper.find('MessageTableEntry');
      expect(messageTableEntry).not.toBeEmptyRender();
    });
  });

  it('renders config fields in table head with correct order', () => {
    const configFields = ['gl2_receive_timestamp', 'user_id', 'gl2_source_input', 'gl2_message_id', 'ingest_time', 'http_method', 'action', 'source', 'ingest_time_hour', 'ingest_time_epoch'];
    const configWithFields = MessagesWidgetConfig.builder().fields(configFields).build();
    const wrapper = mount(<MessageTable activeQueryId={activeQueryId}
                                        config={configWithFields}
                                        fields={Immutable.List(fields)}
                                        onSortChange={() => Promise.resolve()}
                                        selectedFields={{}}
                                        setLoadingState={() => {}}
                                        messages={messages} />);

    const tableHeadFields = wrapper.find('Field').map(field => field.text());
    expect(tableHeadFields).toEqual(configFields);
  });

  it('highlights messsage with id passed in `HighlightMessageContext`', () => {
    const wrapper = mount((
      <HighlightMessageContext.Provider value="message-id-1">
        <MessageTable activeQueryId={activeQueryId}
                      config={config}
                      fields={Immutable.List(fields)}
                      onSortChange={() => Promise.resolve()}
                      selectedFields={Immutable.Set()}
                      setLoadingState={() => {}}
                      messages={messages} />
      </HighlightMessageContext.Provider>
    ));

    const highlightedMessage = wrapper.find('.message-highlight');

    expect(highlightedMessage).toExist();
  });

  it('does not highlight non-existing message id', () => {
    const wrapper = mount((
      <HighlightMessageContext.Provider value="message-id-42">
        <MessageTable activeQueryId={activeQueryId}
                      config={config}
                      fields={Immutable.List(fields)}
                      onSortChange={() => Promise.resolve()}
                      selectedFields={Immutable.Set()}
                      setLoadingState={() => {}}
                      messages={messages} />
      </HighlightMessageContext.Provider>
    ));

    const highlightedMessage = wrapper.find('.message-highlight');

    expect(highlightedMessage).not.toExist();
  });
});
