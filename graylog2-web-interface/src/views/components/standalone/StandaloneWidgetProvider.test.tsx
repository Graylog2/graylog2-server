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

import MessagesWidget from 'views/logic/widgets/MessagesWidget';
import MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import { executeActiveQuery } from 'views/logic/slices/viewSlice';
import { asMock } from 'helpers/mocking';
import useViewsPlugin from 'views/test/testViewsPlugin';

import StandaloneWidgetProvider from './StandaloneWidgetProvider';

jest.mock('views/logic/slices/viewSlice', () => ({
  ...jest.requireActual('views/logic/slices/viewSlice'),
  executeActiveQuery: jest.fn(() => () => Promise.resolve()),
}));
jest.mock('views/logic/slices/createSearch', () => jest.fn(async (search) => search));
jest.mock('views/logic/views/UpdateSearchForWidgets', () => jest.fn((view) => view));
jest.mock('views/logic/fieldtypes/useFieldTypes', () => jest.fn(() => ({ data: [] })));

// Equal, but a new object, as when a page builds the widget from its stored config on every render.
const widget = () =>
  MessagesWidget.builder()
    .id('widget-1')
    .timerange({ type: 'relative', from: 300 })
    .config(MessagesWidgetConfig.builder().fields(['timestamp', 'source']).build())
    .build();

describe('StandaloneWidgetProvider', () => {
  useViewsPlugin();

  it('runs the search once, not again on each render', async () => {
    const { rerender } = render(
      <StandaloneWidgetProvider widget={widget()} title="Messages">
        <span>Widget content</span>
      </StandaloneWidgetProvider>,
    );
    await screen.findByText('Widget content');

    rerender(
      <StandaloneWidgetProvider widget={widget()} title="Messages">
        <span>Widget content</span>
      </StandaloneWidgetProvider>,
    );
    await screen.findByText('Widget content');

    expect(asMock(executeActiveQuery)).toHaveBeenCalledTimes(1);
  });
});
