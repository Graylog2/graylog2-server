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
import { mount } from 'wrappedEnzyme';

import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import Widget from 'views/components/widgets/Widget';
import _Widget from 'views/logic/widgets/Widget';
import type { FieldTypes } from 'views/components/contexts/FieldTypesContext';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import TestStoreProvider from 'views/test/TestStoreProvider';
import useViewsPlugin from 'views/test/testViewsPlugin';
import type View from 'views/logic/views/View';
import { createViewWithWidgets } from 'fixtures/searches';

import WidgetGrid from './WidgetGrid';

jest.mock('./widgets/Widget', () => () => 'widget');

jest.mock('components/common/ReactGridContainer', () => ({ children }) => <span>{children}</span>);

jest.mock('views/components/contexts/WidgetFieldTypesContextProvider', () => ({ children }) => children);

const fieldTypes: FieldTypes = {
  all: Immutable.List(),
  queryFields: Immutable.Map(),
};
const SimpleWidgetGrid = ({ view }: { view?: View }) => (
  <TestStoreProvider view={view}>
    <FieldTypesContext.Provider value={fieldTypes}><WidgetGrid /></FieldTypesContext.Provider>
  </TestStoreProvider>
);

describe('<WidgetGrid />', () => {
  useViewsPlugin();

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
