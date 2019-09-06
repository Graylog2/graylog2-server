// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import ScrollToHint from './common/ScrollToHint';
import { Col, Row } from 'react-bootstrap';
import TimeRangeTypeSelector from './searchbar/TimeRangeTypeSelector';
import { QueriesActions } from '../actions/QueriesActions';
import TimeRangeInput from './searchbar/TimeRangeInput';
import StreamsFilter from './searchbar/StreamsFilter';
import { QueryFiltersActions, QueryFiltersStore } from '../stores/QueryFiltersStore';
import RefreshControls from './searchbar/RefreshControls';
import DocumentationLink from '../../components/support/DocumentationLink';
import DocsHelper from '../../util/DocsHelper';
import SearchButton from './searchbar/SearchButton';
import QueryInput from './searchbar/AsyncQueryInput';
import View from '../logic/views/View';
import * as Immutable from "immutable";
import { Spinner } from '../../components/common/index';
import { ViewStore } from '../stores/ViewStore';
import connect from '../../stores/connect';
import { CurrentQueryStore } from '../stores/CurrentQueryStore';
import { StreamsStore } from '../stores/StreamsStore';

type Props = {
  availableStreams: Array<*>,
  config: any,
  currentQuery: {
    id: string,
    timerange: any,
    query: {
      query_string: string,
    },
  },
  disableSearch: boolean,
  onExecute: (View) => void,
  queryFilters: Immutable.Map,
};

const _performSearch = (onExecute) => {
  const { view } = ViewStore.getInitialState();
  onExecute(view);
};

const DashboardSearchBar = ({ availableStreams, config, currentQuery, disableSearch = false, onExecute, queryFilters }: Props) => {
  if (!currentQuery || !config) {
    return <Spinner />;
  }
  const performSearch = () => _performSearch(onExecute);
  const submitForm = (event) => {
    event.preventDefault();
    performSearch();
  };
  const { timerange, query, id } = currentQuery;
  const { type, ...rest } = timerange;
  const rangeParams = Immutable.Map(rest);
  const rangeType = type;
  const streams = queryFilters.getIn([id, 'filters'], Immutable.List())
    .filter(f => f.get('type') === 'stream')
    .map(f => f.get('id'))
    .toJS();

  return (
    <ScrollToHint value={query.query_string}>
      <Row className="content" style={{ marginRight: 0, marginLeft: 0 }}>
        <Col md={12}>
          <Row className="no-bm">
            <form method="GET" onSubmit={submitForm}>
              <Col md={9}>
                <div className="pull-right search-help">
                  <DocumentationLink page={DocsHelper.PAGES.SEARCH_QUERY_LANGUAGE}
                                     title="Search query syntax documentation"
                                     text={<i className="fa fa-lightbulb-o" />} />
                </div>
                <SearchButton disabled={disableSearch} glyph="filter" />

                <QueryInput value={query.query_string}
                            placeholder="Apply filter to all widgets"
                            onChange={value => QueriesActions.query(id, value).then(performSearch).then(() => value)}
                            onExecute={performSearch} />
              </Col>
              <Col md={3}>
                    <TimeRangeTypeSelector onSelect={newRangeType => QueriesActions.rangeType(id, newRangeType).then(performSearch)}
                                           value={rangeType} />
                    <TimeRangeInput onChange={(key, value) => QueriesActions.rangeParams(id, key, value).then(performSearch)}
                                    rangeType={rangeType}
                                    rangeParams={rangeParams}
                                    config={config} />
              </Col>
            </form>
          </Row>
        </Col>
      </Row>
    </ScrollToHint>
  );
};

DashboardSearchBar.propTypes = {
  config: PropTypes.object.isRequired,
  disableSearch: PropTypes.bool,
  onExecute: PropTypes.func.isRequired,
};

DashboardSearchBar.defaultProps = {
  disableSearch: false,
};

export default connect(
  DashboardSearchBar,
  {
    currentQuery: CurrentQueryStore,
    availableStreams: StreamsStore,
    queryFilters: QueryFiltersStore,
  },
  ({ availableStreams: { streams }, ...rest }) => ({
    ...rest,
    availableStreams: streams.map(stream => ({ key: stream.title, value: stream.id })),
  }),
);
