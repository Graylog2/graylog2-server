import React from 'react';
import { shallow } from 'enzyme';
import Immutable from 'immutable';
import mockComponent from 'helpers/mocking/MockComponent';

import Query from './Query';
import AggregationWidget from '../logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from '../logic/aggregationbuilder/AggregationWidgetConfig';
import WidgetGrid from './WidgetGrid';

jest.mock('components/common', () => ({ Spinner: mockComponent('Spinner') }));
jest.mock('views/logic/Widgets', () => ({ widgetDefinition: () => ({}) }));
jest.mock('views/components/widgets/Widget', () => mockComponent('Widget'));

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
  it('renders extracted results and provided widgets', () => {
    const results = {
      errors: [],
      searchTypes: {
        searchType1: { foo: 23 },
        searchType2: { bar: 42 },
      },
    };
    const wrapper = shallow((
      <Query results={results}
             widgetMapping={widgetMapping}
             widgets={widgets}
             onToggleMessages={() => {}}
             queryId="someQueryId"
             showMessages
             allFields={Immutable.List()}
             fields={Immutable.List()}>
        Sidebar Content
      </Query>
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
    const wrapper = shallow((
      <Query results={results}
             widgetMapping={widgetMapping}
             widgets={widgets}
             onToggleMessages={() => {}}
             queryId="someQueryId"
             showMessages
             allFields={Immutable.List()}
             fields={Immutable.List()}>
        Sidebar Content
      </Query>
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
    const wrapper = shallow((
      <Query results={results}
             widgetMapping={widgetMapping}
             widgets={widgets}
             onToggleMessages={() => {}}
             queryId="someQueryId"
             showMessages
             allFields={Immutable.List()}
             fields={Immutable.List()}>
        Sidebar Content
      </Query>
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
    const wrapper = shallow((
      <Query results={results}
             widgetMapping={widgetMapping}
             widgets={widgets}
             onToggleMessages={() => {}}
             queryId="someQueryId"
             showMessages
             allFields={Immutable.List()}
             fields={Immutable.List()}>
        Sidebar Content
      </Query>
    ));
    const widgetGrid = wrapper.find(WidgetGrid);
    expect(widgetGrid).toHaveLength(1);
    expect(widgetGrid).toHaveProp('errors', { widget1: [error1], widget2: [error2] });
    expect(widgetGrid).toHaveProp('data', { widget1: [], widget2: [] });
    expect(widgetGrid).toHaveProp('widgets', { widget1, widget2 });
  });
});
