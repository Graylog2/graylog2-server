import React from 'react';
import Reflux from 'reflux';
import { Col, Row, ToggleButtonGroup, ToggleButton } from 'react-bootstrap';
import Immutable from 'immutable';

import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';
import Spinner from 'components/common/Spinner';

import SearchButton from 'enterprise/components/searchbar/SearchButton';
import ViewStore from 'enterprise/stores/ViewStore';
import ViewActions from 'enterprise/actions/ViewActions';
import TimeRangeInput from 'enterprise/components/searchbar/TimeRangeInput';
import TimeRangeTypeSelector from 'enterprise/components/searchbar/TimeRangeTypeSelector';
import QueryInput from 'enterprise/components/searchbar/QueryInput';

const SearchBar = React.createClass({
  mixins: [Reflux.connect(ViewStore, 'view')],
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
  _getSavedSearchesSelector() {
  },

  _selectQuery(queryId) {
    ViewActions.selectQuery(queryId);
  },

  render() {
    const selectedQuery = this.state.view.selectedQuery;
    const queries = this.state.view.queries;
    if (queries.size === 0) {
      return <Spinner />;
    }
    const { rangeParams, rangeType, query } = queries.get(selectedQuery);

    const querySelector = (
      <ToggleButtonGroup name="selectedQuery" value={selectedQuery}>
        {queries.valueSeq().map((q, i) => (<ToggleButton key={`query-${q.id}`} value={q.id} onChange={() => this._selectQuery(q.id)}>
          {`Query ${q.id}`}
        </ToggleButton>))}
        <ToggleButton value={-2} onChange={() => ViewActions.removeRootQuery(this.state.view.selectedQuery)}>
          <i className="fa fa-minus" alt="Remove current query" />
        </ToggleButton>
        <ToggleButton value={-1} onChange={() => ViewActions.createRootQuery()}>
          <i className="fa fa-plus" alt="Add a query" />
        </ToggleButton>
      </ToggleButtonGroup>
    );

    return (
      <Row className="no-bm">
        <Col md={12}
             id="universalsearch-container"
             style={{ paddingLeft: '15px', paddingRight: '15px', paddingBottom: '10px' }}>
          <Row className="no-bm">
            <Col md={12} id="universalsearch" style={{ marginTop: '0px' }}>
              <form className="universalsearch-form"
                    method="GET"
                    onSubmit={this._performSearch}>
                <div className="timerange-selector-container">
                  <Row className="no-bm">
                    <Col md={6}>
                      <TimeRangeTypeSelector onSelect={ViewActions.rangeType} value={rangeType} />
                      <TimeRangeInput onChange={ViewActions.rangeParams}
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

                  <SearchButton running={this.state.running} />

                  <QueryInput value={query} onChange={ViewActions.query} />
                </div>
              </form>
            </Col>
          </Row>
        </Col>
      </Row>
    );
  },
});

export default SearchBar;
