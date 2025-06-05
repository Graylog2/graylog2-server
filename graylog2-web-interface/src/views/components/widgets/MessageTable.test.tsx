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
import { render, screen } from 'wrappedTestingLibrary';
import * as Immutable from 'immutable';

import suppressConsole from 'helpers/suppressConsole';
import { MockStore } from 'helpers/mocking';
import MockAction from 'helpers/mocking/MockAction';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldType from 'views/logic/fieldtypes/FieldType';
import MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';

import MessageTable from './MessageTable';

import InteractiveContext from '../contexts/InteractiveContext';
import HighlightMessageContext from '../contexts/HighlightMessageContext';

jest.mock('stores/configurations/ConfigurationsStore', () => ({
  ConfigurationsStore: MockStore(),
  ConfigurationsActions: {
    listSearchesClusterConfig: MockAction(),
  },
}));

jest.mock('views/hooks/useAutoRefresh');

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

const SimpleMessageTable = (props: Partial<Pick<React.ComponentProps<typeof MessageTable>, 'config' | 'fields'>>) => (
  <MessageTable
    activeQueryId={activeQueryId}
    config={config}
    fields={Immutable.List(fields)}
    messages={messages}
    onSortChange={() => Promise.resolve()}
    setLoadingState={() => {}}
    scrollContainerRef={undefined}
    {...props}
  />
);

const highlightedStyle = 'border-left: 7px solid #45E5A8';

describe('MessageTable', () => {
  it('lists provided field in table head', async () => {
    render(<SimpleMessageTable />);

    await screen.findByText(/file_name/i);
  });

  it('renders a table entry for messages', async () => {
    render(<SimpleMessageTable />);

    await screen.findByText(/frank.txt/i);
  });

  it('renders a table entry for messages, even if fields are `undefined`', async () => {
    // Suppressing console to disable props warning because of `fields` being `undefined`.
    await suppressConsole(() => render(<SimpleMessageTable fields={undefined} />));

    await screen.findByText(/file_name/i);
  });

  it('renders config fields in table head with correct order', () => {
    const configFields = [
      'gl2_receive_timestamp',
      'user_id',
      'gl2_source_input',
      'gl2_message_id',
      'ingest_time',
      'http_method',
      'action',
      'source',
      'ingest_time_hour',
      'ingest_time_epoch',
    ];
    const configWithFields = MessagesWidgetConfig.builder().fields(configFields).build();
    render(<SimpleMessageTable config={configWithFields} />);

    configFields.forEach((field) => {
      expect(screen.getByText(field)).toBeInTheDocument();
    });
  });

  it('renders config fields in table head in non interactive mode', () => {
    const configFields = ['gl2_receive_timestamp', 'user_id', 'gl2_source_input'];
    const configWithFields = MessagesWidgetConfig.builder().fields(configFields).build();
    render(
      <InteractiveContext.Provider value={false}>
        <SimpleMessageTable config={configWithFields} />
      </InteractiveContext.Provider>,
    );

    configFields.forEach((field) => {
      expect(screen.getByText(field)).toBeInTheDocument();
    });
  });

  it('highlights message with id passed in `HighlightMessageContext`', async () => {
    render(
      <HighlightMessageContext.Provider value="message-id-1">
        <SimpleMessageTable />
      </HighlightMessageContext.Provider>,
    );

    const message = await screen.findByText('frank.txt');

    expect(message.closest('tbody')).toHaveStyle(highlightedStyle);
  });

  it('does not highlight non-existing message id', async () => {
    render(
      <HighlightMessageContext.Provider value="message-id-42">
        <SimpleMessageTable />
      </HighlightMessageContext.Provider>,
    );

    const message = await screen.findByText('frank.txt');

    expect(message.closest('tbody')).not.toHaveStyle(highlightedStyle);
  });

  it('shows sort icons next to table headers', async () => {
    render(<SimpleMessageTable />);

    await screen.findByText(/frank.txt/i);

    expect(screen.getByText('sort')).toBeInTheDocument();
  });

  it('does not show sort icons in non-interactive context', async () => {
    render(
      <InteractiveContext.Provider value={false}>
        <SimpleMessageTable />
      </InteractiveContext.Provider>,
    );

    await screen.findByText(/frank.txt/i);

    expect(screen.queryByText('sort')).not.toBeInTheDocument();
  });
});
