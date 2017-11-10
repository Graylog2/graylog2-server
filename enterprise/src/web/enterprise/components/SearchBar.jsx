import React from 'react';
import Reflux from 'reflux';
import { Col, Nav, NavItem, Row, Tab } from 'react-bootstrap';
import Immutable from 'immutable';

import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';

import SearchButton from 'enterprise/components/searchbar/SearchButton';
import SearchStore from 'enterprise/stores/SearchStore';
import SearchActions from 'enterprise/actions/SearchActions';
import TimeRangeInput from 'enterprise/components/searchbar/TimeRangeInput';
import TimeRangeTypeSelector from 'enterprise/components/searchbar/TimeRangeTypeSelector';
import QueryInput from 'enterprise/components/searchbar/QueryInput';
import SearchResult from 'enterprise/components/SearchResult';

const SearchBar = React.createClass({
  mixins: [Reflux.connect(SearchStore, 'search')],
  getInitialState() {
    return {
      savedSearch: '',
      keywordPreview: Immutable.Map(),
      selectedQuery: '1',
    };
  },
  _performSearch(event) {
    event.preventDefault();
    const { search } = this.state;
    this.props.onExecute(search.fullQuery);
  },
  _getSavedSearchesSelector() {
  },

  _selectQuery(queryKey) {
    if (queryKey === '__add') {
      console.log('Creating new query');
      // create a new root query, initially sharing the same timerange
      const rootQuery = SearchActions.createRootQuery();
      rootQuery.then((newIndex) => {
        this.setState({ selectedQuery: newIndex });
      });
    } else {
      this.setState({ selectedQuery: queryKey });
    }
  },

  render() {
    const { rangeParams, rangeType, query } = this.state.search;
    const querySelector = (
      <Tab.Container id="query-selector" defaultActiveKey="0" activeKey={this.state.selectedQuery} onSelect={this._selectQuery}>
        <Nav bsStyle="pills">
          <NavItem eventKey="0">
            Query 1
          </NavItem>
          <NavItem eventKey="__add">
            <i className="fa fa-plus" alt="Add a query" />
          </NavItem>
        </Nav>
      </Tab.Container>
    );
    return (
      <Row className="no-bm">
        <Col md={12}
             id="universalsearch-container"
             style={{ paddingLeft: '15px', paddingRight: '15px', paddingBottom: '10px' }}>
          <Row className="no-bm">
            <Col md={8} id="universalsearch" style={{ marginTop: '0px' }}>
              <form className="universalsearch-form"
                    method="GET"
                    onSubmit={this._performSearch}>
                <div className="timerange-selector-container">
                  <Row className="no-bm">
                    <Col md={6}>
                      <TimeRangeTypeSelector onSelect={SearchActions.rangeType} value={rangeType} />
                      <TimeRangeInput onChange={SearchActions.rangeParams}
                                      rangeType={rangeType}
                                      rangeParams={rangeParams}
                                      config={this.props.config} />
                    </Col>
                    <Col md={6}>
                      <div className="saved-searches-selector-container pull-right"
                           style={{ display: 'inline-flex', marginRight: 5 }}>
                        {querySelector}
                      </div>
                    </Col>
                  </Row>
                </div>

                <div id="search-container">
                  <div className="pull-right search-help">
                    <DocumentationLink page={DocsHelper.PAGES.SEARCH_QUERY_LANGUAGE}
                                       title="Search query syntax documentation"
                                       text={<i className="fa fa-lightbulb-o" />} />
                  </div>

                  <SearchButton running />

                  <QueryInput value={query} onChange={SearchActions.query} />
                </div>
              </form>
            </Col>
            <Col md={4}>
              <Row className="no-bm">
                <SearchResult />
              </Row>
            </Col>
          </Row>
        </Col>
      </Row>
    );
  },
});

export default SearchBar;
