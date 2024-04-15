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

import View from 'views/logic/views/View';
import TestStoreProvider from 'views/test/TestStoreProvider';
import useViewsPlugin from 'views/test/testViewsPlugin';
import { createViewWithWidgets, createSearch } from 'fixtures/searches';

import OriginalQuery from './Query';

import AggregationWidget from '../logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from '../logic/aggregationbuilder/AggregationWidgetConfig';

jest.mock('views/components/WidgetGrid', () => () => <span>This is the widget grid</span>);

const Query = (props: Partial<React.ComponentProps<typeof TestStoreProvider>>) => (
  <TestStoreProvider {...props}>
    <OriginalQuery />
  </TestStoreProvider>
);

describe('Query', () => {
  useViewsPlugin();

  it('renders dashboard widget creation explanation on the dashboard page, if no widget is defined', async () => {
    const dashboard = createSearch().toBuilder().type(View.Type.Dashboard).build();

    render(<Query view={dashboard} />);

    await screen.findByText('This dashboard has no widgets yet');
  });

  it('renders search widget creation explanation on the search page, if no widget is defined', async () => {
    const search = createSearch().toBuilder().type(View.Type.Search).build();

    render(<Query view={search} />);

    await screen.findByText('There are no widgets defined to visualize the search result');
  });

  it('renders no widget creation explanation, if there are some widgets defined', async () => {
    const widget1 = AggregationWidget.builder()
      .id('widget1')
      .config(AggregationWidgetConfig.builder().build())
      .build();
    const widget2 = AggregationWidget.builder()
      .id('widget2')
      .config(AggregationWidgetConfig.builder().build())
      .build();
    const viewWithWidgets = createViewWithWidgets([widget1, widget2], {});

    render(<Query view={viewWithWidgets} />);

    await screen.findByText('This is the widget grid');
  });
});
