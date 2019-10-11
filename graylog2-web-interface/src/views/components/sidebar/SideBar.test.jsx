import React from 'react';
import createReactClass from 'create-react-class';
import { mount } from 'enzyme';
import PropTypes from 'prop-types';

import { CombinedProviderMock, StoreMock } from 'helpers/mocking';
import QueryResult from '../../logic/QueryResult';

// eslint-disable-next-line global-require
const loadSUT = () => require('./SideBar');

describe('<Sidebar />', () => {
  let TestComponent;
  let viewMetaData;
  let queryResult;
  let query;

  beforeEach(() => {
    TestComponent = createReactClass({
      propTypes: {
        maximumHeight: PropTypes.number,
      },

      getDefaultProps() {
        return { maximumHeight: undefined };
      },

      getContainerHeight() {
        const { maximumHeight } = this.props;
        return maximumHeight;
      },

      render() {
        expect(this.props).toHaveProperty('maximumHeight');
        return <div id="martian">Marc Watney</div>;
      },
    });

    viewMetaData = {
      activeQuery: '34efae1e-e78e-48ab-ab3f-e83c8611a683',
      description: 'A description',
      id: '5b34f4c44880a54df9616380',
      summary: 'query summary',
      title: 'Query Title',
    };

    // eslint-disable-next-line camelcase
    const effective_timerange = { type: 'absolute', from: '2018-08-28T14:34:26.192Z', to: '2018-08-28T14:39:26.192Z' };
    const duration = 64;
    const timestamp = '2018-08-28T14:39:26.127Z';
    query = {
      filter: { type: 'or', filters: [] },
      id: '34efae1e-e78e-48ab-ab3f-e83c8611a683',
      query: { type: 'elasticsearch', query_string: '*' },
      search_types: [],
      timerange: { type: 'relative', range: 300 },
    };
    const error = [];
    // eslint-disable-next-line camelcase
    const execution_stats = { effective_timerange, duration, timestamp };
    queryResult = new QueryResult({ execution_stats, query, error });

    const currentUser = { timezone: 'UTC' };
    const CurrentUserStore = StoreMock('listen', ['get', () => currentUser], ['getInitialState', () => ({ currentUser })]);
    const SessionStore = StoreMock('listen');
    const combinedProviderMock = new CombinedProviderMock({
      CurrentUser: { CurrentUserStore },
      Session: { SessionStore },
    });
    jest.doMock('injection/CombinedProvider', () => combinedProviderMock);
  });

  it('should render a sidebar', () => {
    const SideBar = loadSUT();
    const wrapper = mount(
      <SideBar viewMetadata={viewMetaData}
               toggleOpen={jest.fn}
               queryId={query.id}
               results={queryResult}>
        <TestComponent />
      </SideBar>,
    );

    wrapper.find('i.sidebarIcon').simulate('click');
    wrapper.find('div[children="View Description"]').simulate('click');

    expect(wrapper.find('h3').text()).toBe(viewMetaData.title);
    expect(wrapper.find('small').text()).toBe(viewMetaData.summary);
  });

  it('should render a sidebar without title and summary', () => {
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

    wrapper.find('i.sidebarIcon').simulate('click');
    wrapper.find('div[children="View Description"]').simulate('click');
    expect(wrapper.find('h3').text()).toBe('New View');
    expect(wrapper.find('small').text()).toBe('No summary.');
    expect(wrapper.find('div.viewMetadata').at(1).text()).toBe('Found 0 messages in 64ms.Query executed at 2018-08-28 14:39:26.');

    wrapper.find('div[children="Create"]').simulate('click');
    expect(wrapper.find('ButtonGroup')).toExist();
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

    wrapper.find('i.sidebarIcon').simulate('click');
    wrapper.find('div[children="Fields"]').simulate('click');
    expect(wrapper.find('div#martian').text()).toBe('Marc Watney');
  });
});
