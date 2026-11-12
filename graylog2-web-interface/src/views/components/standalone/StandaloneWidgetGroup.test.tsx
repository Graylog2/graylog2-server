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
import { useContext } from 'react';
import { render, screen, act } from 'wrappedTestingLibrary';

import MessagesWidget from 'views/logic/widgets/MessagesWidget';
import MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import { executeActiveQuery } from 'views/logic/slices/viewSlice';
import { asMock } from 'helpers/mocking';
import useViewsPlugin from 'views/test/testViewsPlugin';
import WidgetContext from 'views/components/contexts/WidgetContext';
import InteractiveContext from 'views/components/contexts/InteractiveContext';
import createSearch from 'views/logic/slices/createSearch';

import StandaloneWidgetGroup from './StandaloneWidgetGroup';
import StandaloneWidgetContextProvider from './StandaloneWidgetContextProvider';

jest.mock('views/logic/slices/viewSlice', () => ({
  ...jest.requireActual('views/logic/slices/viewSlice'),
  executeActiveQuery: jest.fn(() => () => Promise.resolve()),
}));
jest.mock('views/logic/slices/createSearch', () => jest.fn(async (search) => search));
jest.mock('views/logic/views/UpdateSearchForWidgets', () => jest.fn((view) => view));
jest.mock('views/logic/fieldtypes/useFieldTypes', () => jest.fn(() => ({ data: [] })));

// Equal, but a new object, as when a page builds the widget from its stored config on every render.
const widget = (id = 'widget-1') =>
  MessagesWidget.builder()
    .id(id)
    .timerange({ type: 'relative', from: 300 })
    .config(MessagesWidgetConfig.builder().fields(['timestamp', 'source']).build())
    .build();

describe('StandaloneWidgetGroup', () => {
  useViewsPlugin();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('runs the search once, not again on each render', async () => {
    const { rerender } = render(
      <StandaloneWidgetGroup widgets={[widget()]} titles={{ 'widget-1': 'Messages' }}>
        <span>Widget content</span>
      </StandaloneWidgetGroup>,
    );
    await screen.findByText('Widget content');

    rerender(
      <StandaloneWidgetGroup widgets={[widget()]} titles={{ 'widget-1': 'Messages' }}>
        <span>Widget content</span>
      </StandaloneWidgetGroup>,
    );
    await screen.findByText('Widget content');

    expect(asMock(executeActiveQuery)).toHaveBeenCalledTimes(1);
  });

  it('runs one search for all widgets of the group', async () => {
    const WidgetId = () => <span>Widget {useContext(WidgetContext).id}</span>;

    render(
      <StandaloneWidgetGroup widgets={[widget('widget-1'), widget('widget-2')]}>
        <StandaloneWidgetContextProvider widgetId="widget-1">
          <WidgetId />
        </StandaloneWidgetContextProvider>
        <StandaloneWidgetContextProvider widgetId="widget-2">
          <WidgetId />
        </StandaloneWidgetContextProvider>
      </StandaloneWidgetGroup>,
    );
    await screen.findByText(/widget widget-1/i);
    await screen.findByText(/widget widget-2/i);

    expect(asMock(executeActiveQuery)).toHaveBeenCalledTimes(1);
  });

  it('shows an error when preparing the search fails', async () => {
    asMock(createSearch).mockRejectedValueOnce(new Error('Search could not be stored'));

    render(
      <StandaloneWidgetGroup widgets={[widget()]}>
        <span>Widget content</span>
      </StandaloneWidgetGroup>,
    );

    await screen.findByText(/search could not be stored/i);
    expect(screen.queryByText(/widget content/i)).not.toBeInTheDocument();
  });

  it('shows the widgets read-only by default', async () => {
    const InteractionMode = () => <span>Mode: {useContext(InteractiveContext)}</span>;

    render(
      <StandaloneWidgetGroup widgets={[widget()]}>
        <InteractionMode />
      </StandaloneWidgetGroup>,
    );

    await screen.findByText(/mode: read-only/i);
  });

  it('runs the search of a lazy group only once it comes close to the viewport', async () => {
    render(
      <StandaloneWidgetGroup widgets={[widget()]} lazy>
        <span>Widget content</span>
      </StandaloneWidgetGroup>,
    );

    expect(screen.queryByText(/widget content/i)).not.toBeInTheDocument();
    expect(asMock(executeActiveQuery)).not.toHaveBeenCalled();

    const [reportIntersection] = jest.mocked(window.IntersectionObserver).mock.calls.at(-1);
    act(() => reportIntersection([{ isIntersecting: true } as IntersectionObserverEntry], undefined));

    await screen.findByText(/widget content/i);
    expect(asMock(executeActiveQuery)).toHaveBeenCalledTimes(1);
  });
});
