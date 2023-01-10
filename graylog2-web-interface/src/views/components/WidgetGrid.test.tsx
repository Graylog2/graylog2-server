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
import * as Immutable from 'immutable';
import { Map as MockMap } from 'immutable';
import { mount } from 'wrappedEnzyme';

import { MockStore } from 'helpers/mocking';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import Widget from 'views/components/widgets/Widget';
import _Widget from 'views/logic/widgets/Widget';
import type { FieldTypes } from 'views/components/contexts/FieldTypesContext';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import TestStoreProvider from 'views/test/TestStoreProvider';
import { loadViewsPlugin, unloadViewsPlugin } from 'views/test/testViewsPlugin';
import type View from 'views/logic/views/View';
import { createSearch } from 'fixtures/searches';
import type { WidgetPositions } from 'views/types';

import WidgetGrid from './WidgetGrid';

jest.mock('./widgets/Widget', () => () => 'widget');
// eslint-disable-next-line react/prop-types
jest.mock('components/common/ReactGridContainer', () => ({ children }) => <span>{children}</span>);

jest.mock('views/components/contexts/WidgetFieldTypesContextProvider', () => ({ children }) => children);

jest.mock('views/stores/WidgetStore', () => ({
  WidgetStore: MockStore(['getInitialState', jest.fn()]),
}));

jest.mock('views/stores/CurrentViewStateStore', () => ({
  CurrentViewStateStore: MockStore(['getInitialState', jest.fn(() => ({ state: { widgetPositions: {} } }))]),
}));

jest.mock('views/stores/TitlesStore', () => ({
  TitlesStore: MockStore(['getInitialState', jest.fn(() => MockMap())]),
}));

const fieldTypes: FieldTypes = {
  all: Immutable.List(),
  queryFields: Immutable.Map(),
};
const SimpleWidgetGrid = ({ view }: { view?: View }) => (
  <TestStoreProvider view={view}>
    <FieldTypesContext.Provider value={fieldTypes}><WidgetGrid /></FieldTypesContext.Provider>
  </TestStoreProvider>
);

SimpleWidgetGrid.defaultProps = {
  view: undefined,
};

const createViewWithWidgets = (widgets: Array<_Widget>, positions: WidgetPositions) => {
  const view = createSearch();
  const newViewState = view.state.get('query-id-1')
    .toBuilder()
    .widgets(widgets)
    .widgetPositions(positions)
    .build();

  return view
    .toBuilder()
    .state(Immutable.Map({ 'query-id-1': newViewState }))
    .build();
};

describe('<WidgetGrid />', () => {
  beforeAll(loadViewsPlugin);

  afterAll(unloadViewsPlugin);

  it('should render with minimal props', () => {
    const wrapper = mount(<SimpleWidgetGrid />);

    expect(wrapper).toExist();
  });

  it('should render with widgets passed', () => {
    const widgets = [_Widget.builder().type('dummy').id('widget1').build()];
    const positions = {
      widget1: new WidgetPosition(1, 1, 1, 1),
    };

    const viewWithWidgets = createViewWithWidgets(widgets, positions);

    const wrapper = mount(<SimpleWidgetGrid view={viewWithWidgets} />);

    expect(wrapper.find(Widget)).toHaveLength(1);
  });

  it('should render widget even if widget has no data', () => {
    const widgets = [_Widget.builder().type('dummy').id('widget1').build()];
    const positions = {
      widget1: new WidgetPosition(1, 1, 1, 1),
    };

    const viewWithWidgets = createViewWithWidgets(widgets, positions);

    const wrapper = mount(<SimpleWidgetGrid view={viewWithWidgets} />);

    expect(wrapper.find(Widget)).toHaveLength(1);
  });
});
