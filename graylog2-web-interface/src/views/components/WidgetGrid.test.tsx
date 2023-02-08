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

import { MockStore, asMock } from 'helpers/mocking';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import Widget from 'views/components/widgets/Widget';
import _Widget from 'views/logic/widgets/Widget';
import type { FieldTypes } from 'views/components/contexts/FieldTypesContext';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import ViewState from 'views/logic/views/ViewState';
import mockAction from 'helpers/mocking/MockAction';
import { createSearch as mockCreateSearch } from 'fixtures/searches';
import { ViewStore } from 'views/stores/ViewStore';

import WidgetGrid from './WidgetGrid';

jest.mock('./widgets/Widget', () => () => 'widget');
// eslint-disable-next-line react/prop-types
jest.mock('components/common/ReactGridContainer', () => ({ children }) => <span>{children}</span>);

jest.mock('graylog-web-plugin/plugin', () => ({
  PluginStore: {
    exports: (key) => (key !== 'enterpriseWidgets' ? [] : [
      {
        type: 'dummy',
        displayName: 'Some Dummy Visualization',
        defaultHeight: 5,
        defaultWidth: 6,
      },
    ]),
  },
}));

jest.mock('views/components/contexts/WidgetFieldTypesContextProvider', () => ({ children }) => children);

jest.mock('views/stores/ViewStore', () => ({
  ViewActions: {
    loadNew: mockAction(),
    load: mockAction(),
  },
  ViewStore: MockStore(['getInitialState', jest.fn(() => ({
    view: mockCreateSearch({ queryId: 'foobar' }),
    activeQuery: 'foobar',
  }))]),
}));

describe('<WidgetGrid />', () => {
  beforeEach(() => {
    const activeQuery = 'foobar';
    const widget = _Widget.builder().type('dummy').id('widget1').build();
    const positions = {
      widget1: new WidgetPosition(1, 1, 1, 1),
    };
    const viewState = ViewState.builder()
      .widgets([widget])
      .widgetPositions(positions)
      .build();
    const view = mockCreateSearch({ queryId: activeQuery }).toBuilder()
      .state({ [activeQuery]: viewState })
      .build();
    asMock(ViewStore.getInitialState).mockReturnValue({ view, activeQuery, dirty: false, isNew: false });
  });

  const fieldTypes: FieldTypes = {
    all: Immutable.List(),
    queryFields: Immutable.Map(),
  };
  const SimpleWidgetGrid = () => <FieldTypesContext.Provider value={fieldTypes}><WidgetGrid /></FieldTypesContext.Provider>;

  it('should render with minimal props', () => {
    const wrapper = mount(<SimpleWidgetGrid />);

    expect(wrapper).toExist();
  });

  it('should render with widgets passed', () => {
    const wrapper = mount(<SimpleWidgetGrid />);

    expect(wrapper.find(Widget)).toHaveLength(1);
  });

  it('should render widget even if widget has no data', () => {
    const wrapper = mount(<SimpleWidgetGrid />);

    expect(wrapper.find(Widget)).toHaveLength(1);
  });
});
