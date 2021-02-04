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
import { mount } from 'wrappedEnzyme';
import PropTypes from 'prop-types';
import { StoreMock as MockStore } from 'helpers/mocking';

import ViewTypeContext from 'views/components/contexts/ViewTypeContext';
import View from 'views/logic/views/View';
import QueryResult from 'views/logic/QueryResult';

import Sidebar from './Sidebar';

const mockCurrentUser = { timezone: 'UTC' };

jest.mock('stores/users/CurrentUserStore', () => MockStore(['get', () => mockCurrentUser], ['getInitialState', () => ({ mockCurrentUser })]));

jest.mock('stores/sessions/SessionStore', () => MockStore('isLoggedIn'));

jest.mock('util/AppConfig', () => ({
  gl2AppPathPrefix: jest.fn(() => ''),
  rootTimeZone: jest.fn(() => 'America/Chicago'),
  gl2ServerUrl: jest.fn(() => undefined),
}));

describe('<Sidebar />', () => {
  const viewMetaData = {
    activeQuery: '34efae1e-e78e-48ab-ab3f-e83c8611a683',
    description: 'A description',
    id: '5b34f4c44880a54df9616380',
    summary: 'query summary',
    title: 'Query Title',
  };
  const effectiveTimerange = { type: 'absolute', from: '2018-08-28T14:34:26.192Z', to: '2018-08-28T14:39:26.192Z' };
  const duration = 64;
  const timestamp = '2018-08-28T14:39:26.127Z';
  const query = {
    filter: { type: 'or', filters: [] },
    id: '34efae1e-e78e-48ab-ab3f-e83c8611a683',
    query: { type: 'elasticsearch', query_string: '*' },
    search_types: [],
    timerange: { type: 'relative', from: 300 },
  };
  const errors = [];
  const executionStats = { effective_timerange: effectiveTimerange, duration, timestamp };
  const queryResult = new QueryResult({ execution_stats: executionStats, query, errors });

  class TestComponent extends React.Component {
    static propTypes = {
      maximumHeight: PropTypes.number,
    };

    static defaultProps = {
      maximumHeight: undefined,
    };

    getContainerHeight() {
      const { maximumHeight } = this.props;

      return maximumHeight;
    }

    render() {
      expect(this.props).toHaveProperty('maximumHeight');

      return <div id="martian">Marc Watney</div>;
    }
  }

  it('should render and open when clicking on header', () => {
    const wrapper = mount(
      <Sidebar viewMetadata={viewMetaData}
               viewIsNew={false}
               toggleOpen={jest.fn}
               queryId={query.id}
               results={queryResult}>
        <TestComponent />
      </Sidebar>,
    );

    wrapper.find('SidebarNavigation NavItem').first().simulate('click');

    expect(wrapper.find('h1').text()).toBe(viewMetaData.title);
  });

  it('should render with a description about the query results', () => {
    const wrapper = mount(
      <Sidebar viewMetadata={viewMetaData}
               viewIsNew={false}
               toggleOpen={jest.fn}
               queryId={query.id}
               results={queryResult}>
        <TestComponent />
      </Sidebar>,
    );

    wrapper.find('SidebarNavigation NavItem').first().simulate('click');

    expect(wrapper.find('SearchResultOverview').text()).toBe('Query executed in 64ms at 2018-08-28 09:39:26.');
  });

  it('should render with a specific default title in the context of a new search', () => {
    const emptyViewMetaData = {
      activeQuery: '34efae1e-e78e-48ab-ab3f-e83c8611a683',
      id: '5b34f4c44880a54df9616380',
    };

    const wrapper = mount(
      <ViewTypeContext.Provider value={View.Type.Search}>
        <Sidebar viewMetadata={emptyViewMetaData}
                 viewIsNew={false}
                 toggleOpen={jest.fn}
                 queryId={query.id}
                 results={queryResult}>
          <TestComponent />
        </Sidebar>,
      </ViewTypeContext.Provider>,
    );

    wrapper.find('SidebarNavigation NavItem').first().simulate('click');

    expect(wrapper.find('h1').text()).toBe('Untitled Search');
  });

  it('should render with a specific default title in the context of a new dashboard', () => {
    const emptyViewMetaData = {
      activeQuery: '34efae1e-e78e-48ab-ab3f-e83c8611a683',
      id: '5b34f4c44880a54df9616380',
    };

    const wrapper = mount(
      <ViewTypeContext.Provider value={View.Type.Dashboard}>
        <Sidebar viewMetadata={emptyViewMetaData}
                 viewIsNew={false}
                 toggleOpen={jest.fn}
                 queryId={query.id}
                 results={queryResult}>
          <TestComponent />
        </Sidebar>
      </ViewTypeContext.Provider>,
    );

    wrapper.find('SidebarNavigation NavItem').first().simulate('click');

    expect(wrapper.find('h1').text()).toBe('Untitled Dashboard');
  });

  it('should render with a specific title for unsaved dashboards', () => {
    const emptyViewMetaData = {
      activeQuery: '34efae1e-e78e-48ab-ab3f-e83c8611a683',
      id: '5b34f4c44880a54df9616380',
    };

    const wrapper = mount(
      <ViewTypeContext.Provider value={View.Type.Dashboard}>
        <Sidebar viewMetadata={emptyViewMetaData}
                 viewIsNew
                 toggleOpen={jest.fn}
                 queryId={query.id}
                 results={queryResult}>
          <TestComponent />
        </Sidebar>
      </ViewTypeContext.Provider>,
    );

    wrapper.find('SidebarNavigation NavItem').first().simulate('click');

    expect(wrapper.find('h1').text()).toBe('Unsaved Dashboard');
  });

  it('should render with a specific title for unsaved searches', () => {
    const emptyViewMetaData = {
      activeQuery: '34efae1e-e78e-48ab-ab3f-e83c8611a683',
      id: '5b34f4c44880a54df9616380',
    };

    const wrapper = mount(
      <ViewTypeContext.Provider value={View.Type.Search}>
        <Sidebar viewMetadata={emptyViewMetaData}
                 viewIsNew
                 toggleOpen={jest.fn}
                 queryId={query.id}
                 results={queryResult}>
          <TestComponent />
        </Sidebar>
      </ViewTypeContext.Provider>,
    );

    wrapper.find('SidebarNavigation NavItem').first().simulate('click');

    expect(wrapper.find('h1').text()).toBe('Unsaved Search');
  });

  it('should render summary and description of a view', () => {
    const wrapper = mount(
      <ViewTypeContext.Provider value={View.Type.Dashboard}>
        <Sidebar viewMetadata={viewMetaData}
                 viewIsNew={false}
                 toggleOpen={jest.fn}
                 queryId={query.id}
                 results={queryResult}>
          <TestComponent />
        </Sidebar>
      </ViewTypeContext.Provider>,
    );

    wrapper.find('SidebarNavigation NavItem').first().simulate('click');

    expect(wrapper.find('ViewDescription').text()).toContain(viewMetaData.summary);

    expect(wrapper.find('ViewDescription').text()).toContain(viewMetaData.description);
  });

  it('should render placeholder if dashboard has no summary or description', () => {
    const wrapper = mount(
      <ViewTypeContext.Provider value={View.Type.Dashboard}>
        <Sidebar viewMetadata={{ ...viewMetaData, description: undefined, summary: undefined }}
                 viewIsNew={false}
                 toggleOpen={jest.fn}
                 queryId={query.id}
                 results={queryResult}>
          <TestComponent />
        </Sidebar>
      </ViewTypeContext.Provider>,
    );

    wrapper.find('SidebarNavigation NavItem').first().simulate('click');

    expect(wrapper.find('ViewDescription').text()).toContain('This dashboard has no description');

    expect(wrapper.find('ViewDescription').text()).toContain('This dashboard has no summary');
  });

  it('should render placeholder if saved search has no summary or description', () => {
    const wrapper = mount(
      <ViewTypeContext.Provider value={View.Type.Search}>
        <Sidebar viewMetadata={{ ...viewMetaData, description: undefined, summary: undefined }}
                 viewIsNew={false}
                 toggleOpen={jest.fn}
                 queryId={query.id}
                 results={queryResult}>
          <TestComponent />
        </Sidebar>
      </ViewTypeContext.Provider>,
    );

    wrapper.find('SidebarNavigation NavItem').first().simulate('click');

    expect(wrapper.find('ViewDescription').text()).toContain('This search has no description');

    expect(wrapper.find('ViewDescription').text()).toContain('This search has no summary');
  });

  it('should render a summary and description, for a saved search', () => {
    const wrapper = mount(
      <ViewTypeContext.Provider value={View.Type.Search}>
        <Sidebar viewMetadata={viewMetaData}
                 viewIsNew={false}
                 toggleOpen={jest.fn}
                 queryId={query.id}
                 results={queryResult}>
          <TestComponent />
        </Sidebar>
      </ViewTypeContext.Provider>,
    );

    wrapper.find('SidebarNavigation NavItem').first().simulate('click');

    expect(wrapper.find('ViewDescription').text()).toContain(viewMetaData.summary);

    expect(wrapper.find('ViewDescription').text()).toContain(viewMetaData.description);
  });

  it('should not render a summary and description, if the view is an ad hoc search', () => {
    const wrapper = mount(
      <Sidebar viewMetadata={{ ...viewMetaData, id: undefined }}
               viewIsNew={false}
               toggleOpen={jest.fn}
               queryId={query.id}
               results={queryResult}>
        <TestComponent />
      </Sidebar>,
    );

    wrapper.find('SidebarNavigation NavItem').first().simulate('click');

    expect(wrapper.find('ViewDescription').text()).not.toContain(viewMetaData.summary);

    expect(wrapper.find('ViewDescription').text()).not.toContain(viewMetaData.description);

    expect(wrapper.find('ViewDescription').text()).toContain('Save the search or export it to a dashboard to add a custom summary and description.');
  });

  it('should render widget create options', () => {
    const wrapper = mount(
      <Sidebar viewMetadata={viewMetaData}
               viewIsNew={false}
               toggleOpen={jest.fn}
               queryId={query.id}
               results={queryResult}>
        <TestComponent />
      </Sidebar>,
    );

    wrapper.find('SidebarNavigation NavItem').first().simulate('click');

    wrapper.find('span[children="Create"]').simulate('click');

    expect(wrapper.find('AddWidgetButton').text()).toContain('Predefined Aggregation');
  });

  it('should render passed children', () => {
    const wrapper = mount(
      <Sidebar viewMetadata={viewMetaData}
               viewIsNew={false}
               toggleOpen={jest.fn}
               queryId={query.id}
               results={queryResult}>
        <TestComponent />
      </Sidebar>,
    );

    wrapper.find('SidebarNavigation NavItem').first().simulate('click');

    wrapper.find('span[children="Fields"]').simulate('click');

    expect(wrapper.find('div#martian').text()).toBe('Marc Watney');
  });

  it('should close a section when clicking on its title', () => {
    const wrapper = mount(
      <Sidebar viewMetadata={viewMetaData}
               viewIsNew={false}
               toggleOpen={jest.fn}
               queryId={query.id}
               results={queryResult}>
        <TestComponent />
      </Sidebar>,
    );

    wrapper.find('SidebarNavigation NavItem').first().simulate('click');

    expect(wrapper.find('SearchResultOverview')).toExist();

    wrapper.find('h1').simulate('click');

    expect(wrapper.find('SearchResultOverview')).not.toExist();
  });

  it('should close an active section when clicking on its navigation item', () => {
    const wrapper = mount(
      <Sidebar viewMetadata={viewMetaData}
               viewIsNew={false}
               toggleOpen={jest.fn}
               queryId={query.id}
               results={queryResult}>
        <TestComponent />
      </Sidebar>,
    );

    wrapper.find('SidebarNavigation NavItem').first().simulate('click');
    wrapper.find('div[aria-label="Create"]').simulate('click');

    expect(wrapper.find('h2').text()).toBe('Create');

    wrapper.find('div[aria-label="Create"]').simulate('click');

    expect(wrapper.find('h2')).not.toExist();
  });
});
