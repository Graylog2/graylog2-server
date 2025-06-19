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

import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import Widget from 'views/logic/widgets/Widget';
import TestStoreProvider from 'views/test/TestStoreProvider';
import useViewsPlugin from 'views/test/testViewsPlugin';
import type View from 'views/logic/views/View';
import { createViewWithWidgets } from 'fixtures/searches';
import TestFieldTypesContextProvider from 'views/components/contexts/TestFieldTypesContextProvider';

import WidgetGrid from './WidgetGrid';

jest.mock('./widgets/Widget', () => () => 'rendering widget');

jest.mock('components/common/ReactGridContainer', () => ({ children }) => <span>{children}</span>);

jest.mock(
  'views/components/contexts/WidgetFieldTypesContextProvider',
  () =>
    ({ children }) =>
      children,
);

const SimpleWidgetGrid = ({ view = undefined }: { view?: View }) => (
  <TestStoreProvider view={view}>
    <TestFieldTypesContextProvider>
      <WidgetGrid />
    </TestFieldTypesContextProvider>
  </TestStoreProvider>
);

describe('<WidgetGrid />', () => {
  useViewsPlugin();

  it('should render with minimal props', () => {
    const { container } = render(<SimpleWidgetGrid />);

    expect(container).not.toBeNull();
  });

  it('should render with widgets passed', async () => {
    const widgets = [Widget.builder().type('dummy').id('widget1').build()];
    const positions = {
      widget1: new WidgetPosition(1, 1, 1, 1),
    };

    const viewWithWidgets = createViewWithWidgets(widgets, positions);

    render(<SimpleWidgetGrid view={viewWithWidgets} />);

    expect(await screen.findAllByText('rendering widget')).toHaveLength(1);
  });

  it('should render widget even if widget has no data', async () => {
    const widgets = [Widget.builder().type('dummy').id('widget1').build()];
    const positions = {
      widget1: new WidgetPosition(1, 1, 1, 1),
    };

    const viewWithWidgets = createViewWithWidgets(widgets, positions);

    render(<SimpleWidgetGrid view={viewWithWidgets} />);

    expect(await screen.findAllByText('rendering widget')).toHaveLength(1);
  });
});
