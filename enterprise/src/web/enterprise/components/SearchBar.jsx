import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import Reflux from 'reflux';
import { Col, Row } from 'react-bootstrap';
import Immutable from 'immutable';

import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';
import { Spinner } from 'components/common';

import SearchButton from 'enterprise/components/searchbar/SearchButton';
import TimeRangeInput from 'enterprise/components/searchbar/TimeRangeInput';
import TimeRangeTypeSelector from 'enterprise/components/searchbar/TimeRangeTypeSelector';
import QueryInput from 'enterprise/components/searchbar/QueryInput';
import { QueriesActions } from 'enterprise/stores/QueriesStore';
import { CurrentQueryStore } from 'enterprise/stores/CurrentQueryStore';
import { StreamsStore } from 'enterprise/stores/StreamsStore';
import { QueryFiltersActions, QueryFiltersStore } from 'enterprise/stores/QueryFiltersStore';

import StreamsFilter from './searchbar/StreamsFilter';
import ScrollToHint from './common/ScrollToHint';

const SearchBar = createReactClass({
  displayName: 'SearchBar',

  propTypes: {
    config: PropTypes.object.isRequired,
    disableSearch: PropTypes.bool,
    onExecute: PropTypes.func.isRequired,
  },

  mixins: [
    Reflux.connect(CurrentQueryStore, 'currentQuery'),
    Reflux.connectFilter(StreamsStore, 'availableStreams', ({ streams }) => streams.map(stream => ({ key: stream.title, value: stream.id }))),
    Reflux.connect(QueryFiltersStore, 'queryFilters'),
  ],

  getDefaultProps() {
    return {
      disableSearch: false,
    };
  },

  getInitialState() {
    return {
      savedSearch: '',
      keywordPreview: Immutable.Map(),
    };
  },

  _performSearch(event) {
    event.preventDefault();
    this.props.onExecute();
  },

  render() {
    if (!this.state.currentQuery || !this.props.config) {
      return <Spinner />;
    }
    const { timerange, query, id } = this.state.currentQuery;
    const { type, ...rest } = timerange;
    const rangeParams = Immutable.Map(rest);
    const rangeType = type;
    const streams = this.state.queryFilters.getIn([id, 'filters'], Immutable.List())
      .filter(f => f.get('type') === 'stream')
      .map(f => f.get('id'))
      .toJS();

    return (
      <ScrollToHint value={query.query_string}>
        <Row className="content" style={{ marginRight: 0, marginLeft: 0 }}>
          <Col md={12}>
            <Row className="no-bm">
              <Col md={12}>
                <form method="GET" onSubmit={this._performSearch}>

                  <Row className="no-bm extended-search-query-metadata">
                    <Col md={4}>
                      <TimeRangeTypeSelector onSelect={newRangeType => QueriesActions.rangeType(id, newRangeType).then(this.props.onExecute)}
                                             value={rangeType} />
                      <TimeRangeInput onChange={(key, value) => QueriesActions.rangeParams(id, key, value).then(this.props.onExecute)}
                                      rangeType={rangeType}
                                      rangeParams={rangeParams}
                                      config={this.props.config} />
                    </Col>

                    <Col md={8}>
                      <StreamsFilter value={streams}
                                     streams={this.state.availableStreams}
                                     onChange={value => QueryFiltersActions.streams(id, value)} />
                    </Col>
                  </Row>

                  <Row className="no-bm">
                    <Col md={12}>
                      <div className="pull-right search-help">
                        <DocumentationLink page={DocsHelper.PAGES.SEARCH_QUERY_LANGUAGE}
                                           title="Search query syntax documentation"
                                           text={<i className="fa fa-lightbulb-o" />} />
                      </div>
                      <SearchButton running={this.state.running} disabled={this.props.disableSearch} />

                      <QueryInput value={query.query_string}
                                  placeholder={'Type your search query here and press enter. E.g.: ("not found" AND http) OR http_response_code:[400 TO 404]'}
                                  onChange={value => QueriesActions.query(id, value)}
                                  onExecute={this.props.onExecute} />
                    </Col>
                  </Row>
                </form>
              </Col>
            </Row>
          </Col>
        </Row>
      </ScrollToHint>
    );
  },
});

export default SearchBar;
