import * as React from 'react';
import { render, fireEvent, wait } from 'wrappedTestingLibrary';
import PropTypes from 'prop-types';
import { StoreMock as MockStore } from 'helpers/mocking';

import ViewTypeContext from 'views/components/contexts/ViewTypeContext';
import View from 'views/logic/views/View';
import QueryResult from 'views/logic/QueryResult';
import SearchPageLayoutContext from 'views/components/contexts/SearchPageLayoutContext';

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
  const emptyViewMetaData = {
    activeQuery: '34efae1e-e78e-48ab-ab3f-e83c8611a683',
    id: '5b34f4c44880a54df9616380',
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

  const SimpleSidebar = ({ viewType = View.Type.Search, ...props }) => (
    <ViewTypeContext.Provider value={viewType}>
      <Sidebar viewMetadata={viewMetaData}
               viewIsNew={false}
               toggleOpen={jest.fn}
               queryId={query.id}
               results={queryResult}
               {...props}>
        <TestComponent />
      </Sidebar>,
    </ViewTypeContext.Provider>
  );

  SimpleSidebar.propTypes = {
    viewType: PropTypes.string,
  };

  SimpleSidebar.defaultProps = {
    viewType: View.Type.Search,
  };

  it('should render and open when clicking on header', () => {
    const { getByTitle, queryByText } = render(<SimpleSidebar />);

    fireEvent.click(getByTitle('Open sidebar'));

    expect(queryByText(viewMetaData.title)).not.toBe(null);
  });

  it('should render with a description about the query results', () => {
    const { getByTitle, getByText } = render(<SimpleSidebar />);

    fireEvent.click(getByTitle('Open sidebar'));

    expect(getByText(/Query executed in 64ms/)).not.toBe(null);
    expect(getByText(/2018-08-28 09:39:26/)).not.toBe(null);
  });

  it('should render with a specific default title in the context of a new search', () => {
    const { getByTitle, queryByText } = render(<SimpleSidebar viewMetadata={emptyViewMetaData} />);

    fireEvent.click(getByTitle('Open sidebar'));

    expect(queryByText('Untitled Search')).not.toBe(null);
  });

  it('should render with a specific default title in the context of a new dashboard', () => {
    const { getByTitle, queryByText } = render(<SimpleSidebar viewMetadata={emptyViewMetaData} viewType={View.Type.Dashboard} />);

    fireEvent.click(getByTitle('Open sidebar'));

    expect(queryByText('Untitled Dashboard')).not.toBe(null);
  });

  it('should render with a specific title for unsaved dashboards', () => {
    const { getByTitle, queryByText } = render(<SimpleSidebar viewMetadata={emptyViewMetaData} viewType={View.Type.Dashboard} viewIsNew />);

    fireEvent.click(getByTitle('Open sidebar'));

    expect(queryByText('Unsaved Dashboard')).not.toBe(null);
  });

  it('should render with a specific title for unsaved searches', () => {
    const { getByTitle, queryByText } = render(<SimpleSidebar viewMetadata={emptyViewMetaData} viewIsNew />);

    fireEvent.click(getByTitle('Open sidebar'));

    expect(queryByText('Unsaved Search')).not.toBe(null);
  });

  it('should render summary and description of a view', () => {
    const { getByTitle, queryByText } = render(<SimpleSidebar />);

    fireEvent.click(getByTitle('Open sidebar'));

    expect(queryByText(viewMetaData.summary)).not.toBe(null);
    expect(queryByText(viewMetaData.description)).not.toBe(null);
  });

  it('should render placeholder if dashboard has no summary or description ', () => {
    const { getByTitle, queryByText } = render(<SimpleSidebar viewMetadata={{ ...viewMetaData, description: undefined, summary: undefined }} viewType={View.Type.Dashboard} />);

    fireEvent.click(getByTitle('Open sidebar'));

    expect(queryByText(/This dashboard has no description/)).not.toBe(null);
    expect(queryByText(/This dashboard has no summary/)).not.toBe(null);
  });

  it('should render placeholder if saved search has no summary or description ', () => {
    const { getByTitle, queryByText } = render(<SimpleSidebar viewMetadata={{ ...viewMetaData, description: undefined, summary: undefined }} viewType={View.Type.Search} />);

    fireEvent.click(getByTitle('Open sidebar'));

    expect(queryByText(/This search has no description/)).not.toBe(null);
    expect(queryByText(/This search has no summary/)).not.toBe(null);
  });

  it('should render a summary and description, for a saved search', () => {
    const { getByTitle, queryByText } = render(<SimpleSidebar />);

    fireEvent.click(getByTitle('Open sidebar'));

    expect(queryByText(viewMetaData.summary)).not.toBe(null);
    expect(queryByText(viewMetaData.description)).not.toBe(null);
  });

  it('should not render a summary and description, if the view is an ad hoc search', () => {
    const { getByTitle, queryByText } = render(<SimpleSidebar viewMetadata={{ ...viewMetaData, id: undefined }} />);

    fireEvent.click(getByTitle('Open sidebar'));

    expect(queryByText(viewMetaData.summary)).toBe(null);
    expect(queryByText(viewMetaData.description)).toBe(null);
    expect(queryByText(viewMetaData.description)).not.toBe('Save the search or export it to a dashboard to add a custom summary and description.');
  });

  it('should render widget create options', () => {
    const { getByLabelText, queryByText } = render(<SimpleSidebar />);

    fireEvent.click(getByLabelText('Create'));

    expect(queryByText('Predefined Aggregation')).not.toBe(null);
  });

  it('should render passed children', () => {
    const { getByLabelText, queryByText } = render(<SimpleSidebar />);

    fireEvent.click(getByLabelText('Fields'));

    expect(queryByText('Marc Watney')).not.toBe(null);
  });

  it('should close a section when clicking on its title', () => {
    const { getByTitle, queryByText } = render(<SimpleSidebar />);

    fireEvent.click(getByTitle('Open sidebar'));

    expect(queryByText(viewMetaData.title)).not.toBe(null);

    fireEvent.click(getByTitle('Close sidebar'));

    expect(queryByText(viewMetaData.title)).toBe(null);
  });

  it('should update search page layout on sidebar pinning', async () => {
    const toggleSidebarPinning = jest.fn();
    const layoutConfig = {
      sidebar: {
        isDashboardSidebarPinned: false,
        isSearchSidebarPinned: false,
        isPinned: () => false,
      },
    };
    const { getByTitle } = render(
      <SearchPageLayoutContext.Provider value={{ config: layoutConfig, actions: { toggleSidebarPinning } }}>
        <SimpleSidebar />
      </SearchPageLayoutContext.Provider>,
    );

    fireEvent.click(getByTitle('Open sidebar'));
    fireEvent.click(getByTitle('Display sidebar inline'));

    await wait(() => expect(toggleSidebarPinning).toHaveBeenCalledTimes(1));
  });
});
