import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { Col, Row } from 'react-bootstrap';
import Immutable from 'immutable';

import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';

import SearchButton from 'enterprise/components/searchbar/SearchButton';
import TimeRangeInput from 'enterprise/components/searchbar/TimeRangeInput';
import TimeRangeTypeSelector from 'enterprise/components/searchbar/TimeRangeTypeSelector';
import QueryInput from 'enterprise/components/searchbar/QueryInput';
import QueriesActions from 'enterprise/actions/QueriesActions';
import CurrentViewStore from 'enterprise/stores/CurrentViewStore';
import CombinedProvider from 'injection/CombinedProvider';

import StreamsFilter from './searchbar/StreamsFilter';
import QueryFiltersActions from '../actions/QueryFiltersActions';
import QueryFiltersStore from '../stores/QueryFiltersStore';

const { StreamsStore } = CombinedProvider.get('Streams');

const SearchBar = createReactClass({
  displayName: 'SearchBar',

  mixins: [
    Reflux.connect(CurrentViewStore, 'currentView'),
    Reflux.connect(QueryFiltersStore, 'queryFilters'),
  ],

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

  _getSavedSearchesSelector() {
  },

  render() {
    const { rangeParams, rangeType, query, id } = this.props.query.toObject();
    const { streams } = this.state.queryFilters.get(id, new Immutable.Map()).toObject();
    const { selectedView } = this.state.currentView;

    return (
      <Row className="content" style={{ marginRight: 0, marginLeft: 0 }}>
        <Col md={12}>
          <Row>
            <Col md={12}>
              <form method="GET"
                    onSubmit={this._performSearch}>
                <Row>
                  <Col md={9}>
                    <div className="pull-right search-help">
                      <DocumentationLink page={DocsHelper.PAGES.SEARCH_QUERY_LANGUAGE}
                                         title="Search query syntax documentation"
                                         text={<i className="fa fa-lightbulb-o" />} />
                    </div>
                    <SearchButton running={this.state.running} />

                    <QueryInput value={query}
                                onChange={value => QueriesActions.query(selectedView, id, value)}
                                onExecute={this.props.onExecute}
                                result={this.props.results} />
                  </Col>
                  <Col md={3}>
                    <TimeRangeTypeSelector onSelect={newRangeType => QueriesActions.rangeType(selectedView, id, newRangeType)}
                                           value={rangeType} />
                    <TimeRangeInput onChange={(key, value) => QueriesActions.rangeParams(selectedView, id, key, value)}
                                    rangeType={rangeType}
                                    rangeParams={rangeParams}
                                    config={this.props.config} />
                  </Col>
                </Row>

                <StreamsFilter value={streams}
                               streams={this.state.availableStreams}
                               onChange={value => QueryFiltersActions.streams(selectedView, id, value)} />

              </form>
            </Col>
          </Row>
        </Col>
      </Row>
    );
  },
});

export default SearchBar;
