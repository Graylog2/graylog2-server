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
import suppressConsole from 'helpers/suppressConsole';

import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldType from 'views/logic/fieldtypes/FieldType';
import MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';

import MessageTable from './MessageTable';

import InteractiveContext from '../contexts/InteractiveContext';
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

const SimpleMessageTable = (props) => (
  <WidgetFocusContext.Provider value={{ focusedWidget: undefined, setFocusedWidget: () => {} }}>
    <MessageTable activeQueryId={activeQueryId}
                  config={config}
                  fields={Immutable.List(fields)}
                  messages={messages}
                  onSortChange={() => Promise.resolve()}
                  selectedFields={Immutable.Set()}
                  setLoadingState={() => {}}
                  {...props} />
  </WidgetFocusContext.Provider>
);

describe('MessageTable', () => {
  it('lists provided field in table head', () => {
    const wrapper = mount(<SimpleMessageTable />);
    const th = wrapper.find('th').at(0);

    expect(th.text()).toContain('file_name');
  });

  it('renders a table entry for messages', () => {
    const wrapper = mount(<SimpleMessageTable />);
    const messageTableEntry = wrapper.find('MessageTableEntry');
    const td = messageTableEntry.find('td').at(0);

    expect(td.text()).toContain('frank.txt');
  });

  it('renders a table entry for messages, even if fields are `undefined`', () => {
    // Suppressing console to disable props warning because of `fields` being `undefined`.
    suppressConsole(() => {
      const wrapper = mount(<SimpleMessageTable fields={undefined} />);
      const messageTableEntry = wrapper.find('MessageTableEntry');

      expect(messageTableEntry).not.toBeEmptyRender();
    });
  });

  it('renders config fields in table head with correct order', () => {
    const configFields = ['gl2_receive_timestamp', 'user_id', 'gl2_source_input', 'gl2_message_id', 'ingest_time', 'http_method', 'action', 'source', 'ingest_time_hour', 'ingest_time_epoch'];
    const configWithFields = MessagesWidgetConfig.builder().fields(configFields).build();
    const wrapper = mount(<SimpleMessageTable config={configWithFields} />);

    const tableHeadFields = wrapper.find('Field').map((field) => field.text());

    expect(tableHeadFields).toEqual(configFields);
  });

  it('renders config fields in table head in non interactive mode', () => {
    const configFields = ['gl2_receive_timestamp', 'user_id', 'gl2_source_input'];
    const configWithFields = MessagesWidgetConfig.builder().fields(configFields).build();
    const wrapper = mount(
      <InteractiveContext.Provider value={false}>
        <SimpleMessageTable config={configWithFields} />
      </InteractiveContext.Provider>,
    );

    const tableHeadFields = wrapper.find('Field').map((field) => field.text());

    expect(tableHeadFields).toEqual(configFields);
  });

  it('highlights messsage with id passed in `HighlightMessageContext`', () => {
    const wrapper = mount((
      <HighlightMessageContext.Provider value="message-id-1">
        <SimpleMessageTable />
      </HighlightMessageContext.Provider>
    ));

    const highlightedMessage = wrapper.find('.message-highlight');

    expect(highlightedMessage).toExist();
  });

  it('does not highlight non-existing message id', () => {
    const wrapper = mount((
      <HighlightMessageContext.Provider value="message-id-42">
        <SimpleMessageTable />
      </HighlightMessageContext.Provider>
    ));

    const highlightedMessage = wrapper.find('.message-highlight');

    expect(highlightedMessage).not.toExist();
  });

  it('shows sort icons next to table headers', () => {
    const wrapper = mount(<SimpleMessageTable />);

    const fieldHeader = wrapper.find('th');

    expect(fieldHeader.find('FieldSortIcon')).toExist();
  });

  it('does not show sort icons in non-interactive context', () => {
    const wrapper = mount((
      <InteractiveContext.Provider value={false}>
        <SimpleMessageTable />
      </InteractiveContext.Provider>
    ));

    const fieldHeader = wrapper.find('th');

    expect(fieldHeader.find('FieldSortIcon')).not.toExist();
  });
});
