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
import CombinedProvider from 'injection/CombinedProvider';

import StreamsFilter from './searchbar/StreamsFilter';
import { QueryFiltersActions, QueryFiltersStore } from '../stores/QueryFiltersStore';
import { CurrentQueryStore } from '../stores/CurrentQueryStore';

const { StreamsStore } = CombinedProvider.get('Streams');

const SearchBar = createReactClass({
  displayName: 'SearchBar',

  propTypes: {
    disableSearch: PropTypes.bool,
  },

  mixins: [
    Reflux.connect(CurrentQueryStore, 'currentQuery'),
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
      availableStreams: [],
    };
  },

  componentDidMount() {
    StreamsStore.listStreams().then((streams) => {
      const availableStreams = streams.map(stream => ({ key: stream.title, value: stream.id }));
      this.setState({ availableStreams });
    });
  },

  _performSearch(event) {
    event.preventDefault();
    this.props.onExecute();
  },

  render() {
    if (!this.state.currentQuery) {
      return <Spinner />;
    }
    const { timerange, query, id } = this.state.currentQuery;
    const rangeParams = Immutable.Map({ range: timerange.range });
    const rangeType = timerange.type;
    const streams = this.state.queryFilters.getIn([id, 'filters'], Immutable.List())
      .filter(f => f.get('type') === 'stream')
      .map(f => f.get('id'))
      .toJS();

    return (
      <Row className="content" style={{ marginRight: 0, marginLeft: 0 }}>
        <Col md={12}>
          <Row className="no-bm">
            <Col md={12}>
              <form method="GET" onSubmit={this._performSearch}>

                <Row className="no-bm extended-search-query-metadata">
                  <Col md={4}>
                    <TimeRangeTypeSelector onSelect={newRangeType => QueriesActions.rangeType(id, newRangeType)}
                                           value={rangeType} />
                    <TimeRangeInput onChange={(key, value) => QueriesActions.rangeParams(id, key, value)}
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
                                onExecute={this.props.onExecute}
                                result={this.props.results} />
                  </Col>
                </Row>
              </form>
            </Col>
          </Row>
        </Col>
      </Row>
    );
  },
});

export default SearchBar;
