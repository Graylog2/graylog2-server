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
import React from 'react';
import { mount } from 'wrappedEnzyme';
import Immutable from 'immutable';
import mockComponent from 'helpers/mocking/MockComponent';

import ViewTypeContext from 'views/components/contexts/ViewTypeContext';
import View from 'views/logic/views/View';

import Query from './Query';
import WidgetGrid from './WidgetGrid';
import WidgetFocusContext from './contexts/WidgetFocusContext';

import AggregationWidget from '../logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from '../logic/aggregationbuilder/AggregationWidgetConfig';

jest.mock('components/common', () => ({ Spinner: mockComponent('Spinner') }));
jest.mock('views/logic/Widgets', () => ({ widgetDefinition: () => ({}) }));
jest.mock('views/components/widgets/Widget', () => mockComponent('Widget'));
jest.mock('views/components/WidgetGrid', () => mockComponent('WidgetGrid'));

const widgetMapping = Immutable.Map([
  ['widget1', ['searchType1']],
  ['widget2', ['searchType2']],
]);
const widget1 = AggregationWidget.builder()
  .id('widget1')
  .config(AggregationWidgetConfig.builder().build())
  .build();
const widget2 = AggregationWidget.builder()
  .id('widget2')
  .config(AggregationWidgetConfig.builder().build())
  .build();
const widgets = Immutable.Map({ widget1, widget2 });

describe('Query', () => {
  const SUT = (props) => (
    <WidgetFocusContext.Provider value={{ focusedWidget: undefined, setFocusedWidget: () => {} }}>
      <Query results={{ errors: [], searchTypes: {} }}
             widgetMapping={widgetMapping}
             widgets={widgets}
             onToggleMessages={() => {}}
             queryId="someQueryId"
             showMessages
             allFields={Immutable.List()}
             fields={Immutable.List()}
             {...props} />
    </WidgetFocusContext.Provider>
  );

  it('renders extracted results and provided widgets', () => {
    const results = {
      errors: [],
      searchTypes: {
        searchType1: { foo: 23 },
        searchType2: { bar: 42 },
      },
    };
    const wrapper = mount((
      <SUT results={results} />
    ));
    const widgetGrid = wrapper.find(WidgetGrid);

    expect(widgetGrid).toHaveLength(1);
    expect(widgetGrid).toHaveProp('errors', {});
    expect(widgetGrid).toHaveProp('data', { widget1: [{ foo: 23 }], widget2: [{ bar: 42 }] });
    expect(widgetGrid).toHaveProp('widgets', { widget1, widget2 });
  });

  it('renders extracted partial result, partial error and provided widgets', () => {
    const error = { searchTypeId: 'searchType1', description: 'This is a very specific error.' };
    const results = {
      errors: [error],
      searchTypes: {
        searchType2: { bar: 42 },
      },
    };
    const wrapper = mount((
      <SUT results={results} />
    ));
    const widgetGrid = wrapper.find(WidgetGrid);

    expect(widgetGrid).toHaveLength(1);
    expect(widgetGrid).toHaveProp('errors', { widget1: [error] });
    expect(widgetGrid).toHaveProp('data', { widget1: [], widget2: [{ bar: 42 }] });
    expect(widgetGrid).toHaveProp('widgets', { widget1, widget2 });
  });

  it('renders extracted partial result, multiple errors and provided widgets', () => {
    const error1 = { searchTypeId: 'searchType2', description: 'This is a very specific error.' };
    const error2 = { searchTypeId: 'searchType2', description: 'This is another very specific error.' };
    const results = {
      errors: [error1, error2],
      searchTypes: {
        searchType1: { foo: 17 },
      },
    };
    const wrapper = mount((
      <SUT results={results} />
    ));
    const widgetGrid = wrapper.find(WidgetGrid);

    expect(widgetGrid).toHaveLength(1);
    expect(widgetGrid).toHaveProp('errors', { widget2: [error1, error2] });
    expect(widgetGrid).toHaveProp('data', { widget1: [{ foo: 17 }], widget2: [] });
    expect(widgetGrid).toHaveProp('widgets', { widget1, widget2 });
  });

  it('renders errors for all components and provided widgets', () => {
    const error1 = { searchTypeId: 'searchType1', description: 'This is a very specific error.' };
    const error2 = { searchTypeId: 'searchType2', description: 'This is another very specific error.' };
    const results = {
      errors: [error1, error2],
      searchTypes: {},
    };
    const wrapper = mount((
      <SUT results={results} />
    ));
    const widgetGrid = wrapper.find(WidgetGrid);

    expect(widgetGrid).toHaveLength(1);
    expect(widgetGrid).toHaveProp('errors', { widget1: [error1], widget2: [error2] });
    expect(widgetGrid).toHaveProp('data', { widget1: [], widget2: [] });
    expect(widgetGrid).toHaveProp('widgets', { widget1, widget2 });
  });

  it('renders dashboard widget creation explanation on the dashboard page, if no widget is defined', () => {
    const results = {
      errors: [],
      searchTypes: {},
    };
    const wrapper = mount((
      <ViewTypeContext.Provider value={View.Type.Dashboard}>
        <SUT results={results}
             widgets={Immutable.Map()} />
      </ViewTypeContext.Provider>
    ));

    expect(wrapper.html()).toContain('<h2>This dashboard has no widgets yet</h2>');
    expect(wrapper.html()).toContain('4. <b>Share</b> the dashboard with your colleagues.');
  });

  it('renders search widget creation explanation on the search page, if no widget is defined', () => {
    const results = {
      errors: [],
      searchTypes: {},
    };
    const wrapper = mount((
      <ViewTypeContext.Provider value={View.Type.Search}>
        <SUT results={results}
             widgets={Immutable.Map()} />
      </ViewTypeContext.Provider>
    ));

    expect(wrapper.html()).toContain('<h2>There are no widgets defined to visualize the search result</h2>');
    expect(wrapper.html()).not.toContain('4. <b>Share</b> the dashboard with your colleagues.');
  });

  it('renders no widget creation explanation, if there are some widgets defined', () => {
    const results = {
      errors: [],
      searchTypes: {},
    };
    const wrapper = mount((
      <SUT results={results} />
    ));

    expect(wrapper.contains('You can create a new widget by selecting a widget type')).toEqual(false);
  });
});
