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
import Immutable, { Map as MockMap } from 'immutable';

import { MockStore, asMock } from 'helpers/mocking';
import ViewTypeContext from 'views/components/contexts/ViewTypeContext';
import type { ViewType } from 'views/logic/views/View';
import View from 'views/logic/views/View';
import { WidgetStore } from 'views/stores/WidgetStore';

import Query from './Query';

import AggregationWidget from '../logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from '../logic/aggregationbuilder/AggregationWidgetConfig';

jest.mock('views/components/WidgetGrid', () => () => <span>This is the widget grid</span>);

jest.mock('views/stores/WidgetStore', () => ({
  WidgetStore: MockStore(['getInitialState', jest.fn(() => MockMap())]),
}));

describe('Query', () => {
  const SimpleQuery = ({ type }: { type: ViewType }) => (
    <ViewTypeContext.Provider value={type}>
      <Query />
    </ViewTypeContext.Provider>
  );

  it('renders dashboard widget creation explanation on the dashboard page, if no widget is defined', async () => {
    render(<SimpleQuery type={View.Type.Dashboard} />);

    await screen.findByText('This dashboard has no widgets yet');
  });

  it('renders search widget creation explanation on the search page, if no widget is defined', async () => {
    render(<SimpleQuery type={View.Type.Search} />);

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
    const widgets = Immutable.Map({ widget1, widget2 });
    asMock(WidgetStore.getInitialState).mockReturnValue(widgets);

    render(<SimpleQuery type={View.Type.Search} />);

    await screen.findByText('This is the widget grid');
  });
});
