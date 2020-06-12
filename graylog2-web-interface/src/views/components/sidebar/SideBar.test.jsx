import React from 'react';
import { mount } from 'wrappedEnzyme';
import PropTypes from 'prop-types';

import { StoreMock as MockStore } from 'helpers/mocking';
import ViewTypeContext from 'views/components/contexts/ViewTypeContext';
import View from 'views/logic/views/View';
import QueryResult from 'views/logic/QueryResult';
import SideBar from './SideBar';

const mockCurrentUser = { timezone: 'UTC' };
jest.mock('stores/users/CurrentUserStore', () => MockStore(['get', () => mockCurrentUser], ['getInitialState', () => ({ mockCurrentUser })]));
jest.mock('stores/sessions/SessionStore', () => MockStore('isLoggedIn'));

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
    timerange: { type: 'relative', range: 300 },
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
      <SideBar viewMetadata={viewMetaData}
               toggleOpen={jest.fn}
               queryId={query.id}
               results={queryResult}>
        <TestComponent />
      </SideBar>,
    );

    wrapper.find('Sidebarstyles__SidebarHeader').simulate('click');
    expect(wrapper.find('h3').text()).toBe(viewMetaData.title);
  });

  it('should render with a description', () => {
    const emptyViewMetaData = {
      activeQuery: '34efae1e-e78e-48ab-ab3f-e83c8611a683',
      id: '5b34f4c44880a54df9616380',
    };

    const wrapper = mount(
      <SideBar viewMetadata={emptyViewMetaData}
               toggleOpen={jest.fn}
               queryId={query.id}
               results={queryResult}>
        <TestComponent />
      </SideBar>,
    );

    wrapper.find('Sidebarstyles__SidebarHeader').simulate('click');
    wrapper.find('div[children="Description"]').simulate('click');
    expect(wrapper.find('SearchResultOverview').text()).toBe('Query executed in 64ms at 2018-08-28 14:39:26.');
  });

  it('should render with a specific default title in the context of a new search', () => {
    const emptyViewMetaData = {
      activeQuery: '34efae1e-e78e-48ab-ab3f-e83c8611a683',
      id: '5b34f4c44880a54df9616380',
    };

    const wrapper = mount(
      <ViewTypeContext.Provider value={View.Type.Search}>
        <SideBar viewMetadata={emptyViewMetaData}
                 toggleOpen={jest.fn}
                 queryId={query.id}
                 results={queryResult}>
          <TestComponent />
        </SideBar>,
      </ViewTypeContext.Provider>,
    );

    wrapper.find('Sidebarstyles__SidebarHeader').simulate('click');
    expect(wrapper.find('h3').text()).toBe('Untitled Search');
  });

  it('should render with a specific default title in the context of a new dashboard', () => {
    const emptyViewMetaData = {
      activeQuery: '34efae1e-e78e-48ab-ab3f-e83c8611a683',
      id: '5b34f4c44880a54df9616380',
    };

    const wrapper = mount(
      <ViewTypeContext.Provider value={View.Type.Dashboard}>
        <SideBar viewMetadata={emptyViewMetaData}
                 toggleOpen={jest.fn}
                 queryId={query.id}
                 results={queryResult}>
          <TestComponent />
        </SideBar>
      </ViewTypeContext.Provider>,
    );

    wrapper.find('Sidebarstyles__SidebarHeader').simulate('click');
    expect(wrapper.find('h3').text()).toBe('Untitled Dashboard');
  });

  it('should render summary and description of a view', () => {
    const wrapper = mount(
      <ViewTypeContext.Provider value={View.Type.Dashboard}>
        <SideBar viewMetadata={viewMetaData}
                 toggleOpen={jest.fn}
                 queryId={query.id}
                 results={queryResult}>
          <TestComponent />
        </SideBar>
      </ViewTypeContext.Provider>,
    );

    wrapper.find('Sidebarstyles__SidebarHeader').simulate('click');
    wrapper.find('div[children="Description"]').simulate('click');
    expect(wrapper.find('ViewDescription').text()).toContain(viewMetaData.summary);
    expect(wrapper.find('ViewDescription').text()).toContain(viewMetaData.description);
  });

  it('should render placeholder if dashboard has no summary or description ', () => {
    const wrapper = mount(
      <ViewTypeContext.Provider value={View.Type.Dashboard}>
        <SideBar viewMetadata={{ ...viewMetaData, description: undefined, summary: undefined }}
                 toggleOpen={jest.fn}
                 queryId={query.id}
                 results={queryResult}>
          <TestComponent />
        </SideBar>
      </ViewTypeContext.Provider>,
    );

    wrapper.find('Sidebarstyles__SidebarHeader').simulate('click');
    wrapper.find('div[children="Description"]').simulate('click');
    expect(wrapper.find('ViewDescription').text()).toContain('No dashboard description');
    expect(wrapper.find('ViewDescription').text()).toContain('No dashboard summary');
  });

  it('should render placeholder if saved search has no summary or description ', () => {
    const wrapper = mount(
      <ViewTypeContext.Provider value={View.Type.Search}>
        <SideBar viewMetadata={{ ...viewMetaData, description: undefined, summary: undefined }}
                 toggleOpen={jest.fn}
                 queryId={query.id}
                 results={queryResult}>
          <TestComponent />
        </SideBar>
      </ViewTypeContext.Provider>,
    );

    wrapper.find('Sidebarstyles__SidebarHeader').simulate('click');
    wrapper.find('div[children="Description"]').simulate('click');
    expect(wrapper.find('ViewDescription').text()).toContain('No search description');
    expect(wrapper.find('ViewDescription').text()).toContain('No search summary');
  });

  it('should render a summary and description, for a saved search', () => {
    const wrapper = mount(
      <ViewTypeContext.Provider value={View.Type.Search}>
        <SideBar viewMetadata={viewMetaData}
                 toggleOpen={jest.fn}
                 queryId={query.id}
                 results={queryResult}>
          <TestComponent />
        </SideBar>
      </ViewTypeContext.Provider>,
    );

    wrapper.find('Sidebarstyles__SidebarHeader').simulate('click');
    wrapper.find('div[children="Description"]').simulate('click');
    expect(wrapper.find('ViewDescription').text()).toContain(viewMetaData.summary);
    expect(wrapper.find('ViewDescription').text()).toContain(viewMetaData.description);
  });

  it('should not render a summary and description, if the view is an ad hoc search', () => {
    const wrapper = mount(
      <SideBar viewMetadata={{ ...viewMetaData, id: undefined }}
               toggleOpen={jest.fn}
               queryId={query.id}
               results={queryResult}>
        <TestComponent />
      </SideBar>,
    );

    wrapper.find('Sidebarstyles__SidebarHeader').simulate('click');
    wrapper.find('div[children="Description"]').simulate('click');
    expect(wrapper.find('ViewDescription').text()).not.toContain(viewMetaData.summary);
    expect(wrapper.find('ViewDescription').text()).not.toContain(viewMetaData.description);
    expect(wrapper.find('ViewDescription').text()).toContain('Save the search or export it to a dashboard to add a custom description.');
  });

  it('should render widget create options', () => {
    const wrapper = mount(
      <SideBar viewMetadata={viewMetaData}
               toggleOpen={jest.fn}
               queryId={query.id}
               results={queryResult}>
        <TestComponent />
      </SideBar>,
    );

    wrapper.find('Sidebarstyles__SidebarHeader').simulate('click');
    wrapper.find('div[children="Create"]').simulate('click');
    expect(wrapper.find('AddWidgetButton').text()).toContain('Predefined Aggregation');
  });

  it('should render passed children', () => {
    const wrapper = mount(
      <SideBar viewMetadata={viewMetaData}
               toggleOpen={jest.fn}
               queryId={query.id}
               results={queryResult}>
        <TestComponent />
      </SideBar>,
    );

    wrapper.find('Sidebarstyles__SidebarHeader').simulate('click');
    wrapper.find('div[children="Fields"]').simulate('click');
    expect(wrapper.find('div#martian').text()).toBe('Marc Watney');
  });

  it('should close a section when clicking on its title', () => {
    const wrapper = mount(
      <SideBar viewMetadata={viewMetaData}
               toggleOpen={jest.fn}
               queryId={query.id}
               results={queryResult}>
        <TestComponent />
      </SideBar>,
    );

    wrapper.find('Sidebarstyles__SidebarHeader').simulate('click');
    wrapper.find('div[children="Description"]').simulate('click');
    expect(wrapper.find('SearchResultOverview')).toExist();
    wrapper.find('div[children="Description"]').simulate('click');
    expect(wrapper.find('SearchResultOverview')).not.toExist();
  });
});
