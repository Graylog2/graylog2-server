// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';

import connect from 'stores/connect';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';
import { Spinner, Icon } from 'components/common';
import { Col, Row } from 'components/graylog';

import SearchButton from 'views/components/searchbar/SearchButton';
import BookmarkControls from 'views/components/searchbar/bookmark/BookmarkControls';
import TimeRangeInput from 'views/components/searchbar/TimeRangeInput';
import TimeRangeTypeSelector from 'views/components/searchbar/TimeRangeTypeSelector';
import QueryInput from 'views/components/searchbar/AsyncQueryInput';
import StreamsFilter from 'views/components/searchbar/StreamsFilter';
import RefreshControls from 'views/components/searchbar/RefreshControls';
import ScrollToHint from 'views/components/common/ScrollToHint';
import HorizontalSpacer from 'views/components/horizontalspacer/HorizontalSpacer';
import { QueriesActions } from 'views/stores/QueriesStore';
import { CurrentQueryStore } from 'views/stores/CurrentQueryStore';
import { StreamsStore } from 'views/stores/StreamsStore';
import { QueryFiltersActions, QueryFiltersStore } from 'views/stores/QueryFiltersStore';

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
  onExecute: () => void,
  queryFilters: Immutable.Map,
};

const SearchBar = ({ availableStreams, config, currentQuery, disableSearch = false, onExecute: performSearch, queryFilters }: Props) => {
  if (!currentQuery || !config) {
    return <Spinner />;
  }
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
            <Col md={12}>
              <form method="GET" onSubmit={submitForm}>

                <Row className="no-bm extended-search-query-metadata">
                  <Col md={4}>
                    <TimeRangeTypeSelector onSelect={newRangeType => QueriesActions.rangeType(id, newRangeType).then(performSearch)}
                                           value={rangeType} />
                    <TimeRangeInput onChange={(key, value) => QueriesActions.rangeParams(id, key, value).then(performSearch)}
                                    rangeType={rangeType}
                                    rangeParams={rangeParams}
                                    config={config} />
                  </Col>

                  <Col mdHidden lgHidden>
                    <HorizontalSpacer />
                  </Col>

                  <Col md={5} xs={8}>
                    <StreamsFilter value={streams}
                                   streams={availableStreams}
                                   onChange={value => QueryFiltersActions.streams(id, value)} />
                  </Col>

                  <Col md={3} xs={4}>
                    <RefreshControls />
                  </Col>
                </Row>

                <Row className="no-bm">
                  <Col md={10} xs={9}>
                    <div className="pull-right search-help">
                      <DocumentationLink page={DocsHelper.PAGES.SEARCH_QUERY_LANGUAGE}
                                         title="Search query syntax documentation"
                                         text={<Icon name="lightbulb-o" />} />
                    </div>
                    <SearchButton disabled={disableSearch} />

                    <QueryInput value={query.query_string}
                                placeholder={'Type your search query here and press enter. E.g.: ("not found" AND http) OR http_response_code:[400 TO 404]'}
                                onChange={value => QueriesActions.query(id, value).then(performSearch).then(() => value)}
                                onExecute={performSearch} />
                  </Col>
                  <Col md={2} xs={3}>
                    <BookmarkControls />
                  </Col>
                </Row>
              </form>
            </Col>
          </Row>
        </Col>
      </Row>
    </ScrollToHint>
  );
};

SearchBar.propTypes = {
  config: PropTypes.object.isRequired,
  disableSearch: PropTypes.bool,
  onExecute: PropTypes.func.isRequired,
};

SearchBar.defaultProps = {
  disableSearch: false,
};

export default connect(
  SearchBar,
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
