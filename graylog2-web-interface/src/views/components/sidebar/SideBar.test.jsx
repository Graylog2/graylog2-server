import React from 'react';
import { mount } from 'wrappedEnzyme';
import PropTypes from 'prop-types';

import ViewTypeContext from 'views/components/contexts/ViewTypeContext';
import { CombinedProviderMock, StoreMock } from 'helpers/mocking';
import View from 'views/logic/views/View';
import QueryResult from '../../logic/QueryResult';

// eslint-disable-next-line global-require
const loadSUT = () => require('./SideBar');

describe('<Sidebar />', () => {
  let TestComponent;
  let viewMetaData;
  let queryResult;
  let query;

  beforeEach(() => {
    TestComponent = class TestComponentClass extends React.Component {
      static propTypes = {
        maximumHeight: PropTypes.number,
      }

      static defaultProps = {
        maximumHeight: undefined,
      }

      getContainerHeight() {
        const { maximumHeight } = this.props;
        return maximumHeight;
      }

      render() {
        expect(this.props).toHaveProperty('maximumHeight');
        return <div id="martian">Marc Watney</div>;
      }
    };

    viewMetaData = {
      activeQuery: '34efae1e-e78e-48ab-ab3f-e83c8611a683',
      description: 'A description',
      id: '5b34f4c44880a54df9616380',
      summary: 'query summary',
      title: 'Query Title',
    };

    const effectiveTimerange = { type: 'absolute', from: '2018-08-28T14:34:26.192Z', to: '2018-08-28T14:39:26.192Z' };
    const duration = 64;
    const timestamp = '2018-08-28T14:39:26.127Z';
    query = {
      filter: { type: 'or', filters: [] },
      id: '34efae1e-e78e-48ab-ab3f-e83c8611a683',
      query: { type: 'elasticsearch', query_string: '*' },
      search_types: [],
      timerange: { type: 'relative', range: 300 },
    };
    const errors = [];
    const executionStats = { effective_timerange: effectiveTimerange, duration, timestamp };
    queryResult = new QueryResult({ execution_stats: executionStats, query, errors });

    const currentUser = { timezone: 'UTC' };
    const CurrentUserStore = StoreMock('listen', ['get', () => currentUser], ['getInitialState', () => ({ currentUser })]);
    const SessionStore = StoreMock('listen');
    const combinedProviderMock = new CombinedProviderMock({
      CurrentUser: { CurrentUserStore },
      Session: { SessionStore },
    });
    jest.doMock('injection/CombinedProvider', () => combinedProviderMock);
  });

  it('should render and open when clicking on header', () => {
    const SideBar = loadSUT();
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

    const SideBar = loadSUT();
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

    const SideBar = loadSUT();
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

    const SideBar = loadSUT();
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

  it('should render a summary and descirption, for dashboard view', () => {
    const SideBar = loadSUT();
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

  it('should not render a summary and descirption, if the view is not a dashboard', () => {
    const SideBar = loadSUT();
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
    expect(wrapper.find('ViewDescription').text()).not.toContain(viewMetaData.summary);
    expect(wrapper.find('ViewDescription').text()).not.toContain(viewMetaData.description);
  });

  it('should render widget create options', () => {
    const SideBar = loadSUT();
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
    const SideBar = loadSUT();
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
});
