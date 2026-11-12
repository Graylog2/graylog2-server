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
import { render } from 'wrappedTestingLibrary';

import { asMock } from 'helpers/mocking';
import { AssistantPageContextProvider, useAssistantPageSnapshot } from 'components/assistant';
import type { PageTools } from 'components/assistant';
import useViewsSelector from 'views/stores/useViewsSelector';
import useViewsDispatch from 'views/stores/useViewsDispatch';
import { selectActiveQuery, selectView } from 'views/logic/slices/viewSelectors';
import { addWidget } from 'views/logic/slices/widgetActions';
import { setTitle } from 'views/logic/slices/titlesActions';
import { createSearch } from 'fixtures/searches';
import type Widget from 'views/logic/widgets/Widget';
import MessagesWidget from 'views/logic/widgets/MessagesWidget';
import TextWidget from 'views/logic/widgets/TextWidget';
import EventsWidget from 'views/logic/widgets/events/EventsWidget';

import SearchPageAssistantContext from './SearchPageAssistantContext';

jest.mock('routing/useLocation', () => () => ({ pathname: '/search' }));
jest.mock('views/stores/useViewsSelector');
jest.mock('views/stores/useViewsDispatch');
jest.mock('views/logic/slices/widgetActions', () => ({ addWidget: jest.fn(() => 'addWidget') }));
jest.mock('views/logic/slices/titlesActions', () => ({ setTitle: jest.fn(() => 'setTitle') }));

const renderTools = () => {
  let getTools: () => PageTools;
  const Tools = () => {
    getTools = useAssistantPageSnapshot().getTools;

    return null;
  };

  render(
    <AssistantPageContextProvider>
      <SearchPageAssistantContext />
      <Tools />
    </AssistantPageContextProvider>,
  );

  return () => getTools();
};

const addedWidget = () => asMock(addWidget).mock.calls[0][0] as Widget;

describe('SearchPageAssistantContext', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    const view = createSearch();
    asMock(useViewsSelector).mockImplementation((selector) => {
      if (selector === selectView) return view;
      if (selector === selectActiveQuery) return 'query-id';

      return undefined;
    });
    asMock(useViewsDispatch).mockReturnValue(jest.fn(async () => {}) as unknown as ReturnType<typeof useViewsDispatch>);
  });

  it('adds a message table with the given columns, sort and search', async () => {
    const tools = renderTools();

    await tools().add_message_table({
      title: 'Failed logins',
      query: 'action:login_failed',
      fields: ['timestamp', 'user_name'],
      sort: { field: 'user_name', direction: 'Ascending' },
    });

    const widget = addedWidget() as MessagesWidget;

    expect(widget).toBeInstanceOf(MessagesWidget);
    expect(widget.config.fields).toEqual(['timestamp', 'user_name']);
    expect(widget.config.sort[0].field).toBe('user_name');
    expect(widget.config.sort[0].direction.direction).toBe('Ascending');
    expect(widget.query).toEqual({ type: 'elasticsearch', query_string: 'action:login_failed' });
    expect(setTitle).toHaveBeenCalledWith('query-id', 'widget', widget.id, 'Failed logins');
  });

  it('adds a message table with the default columns', async () => {
    const tools = renderTools();

    await tools().add_message_table({ title: 'Messages' });

    expect((addedWidget() as MessagesWidget).config.fields).toEqual(['timestamp', 'source']);
  });

  it('adds a text widget', async () => {
    const tools = renderTools();

    const result = await tools().add_text_widget({ title: 'Notes', text: '# Hey there!' });

    expect(addedWidget()).toBeInstanceOf(TextWidget);
    expect(addedWidget().config.text).toBe('# Hey there!');
    expect(result).toEqual(expect.objectContaining({ summary: 'Added "Notes"' }));
  });

  it('adds an events widget with its mode and filters', async () => {
    const tools = renderTools();

    await tools().add_events_widget({
      title: 'Critical alerts',
      mode: 'Number',
      filters: [{ field: 'priority', value: ['4'] }],
    });

    const widget = addedWidget() as EventsWidget;

    expect(widget).toBeInstanceOf(EventsWidget);
    expect(widget.config.mode).toBe('Number');
    expect(widget.config.filters.toArray()).toEqual([{ field: 'priority', value: ['4'] }]);
  });
});
