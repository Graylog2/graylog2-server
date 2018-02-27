import React from 'react';
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

const SearchBar = React.createClass({
  mixins: [
    Reflux.connect(CurrentViewStore, 'currentView'),
  ],
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

  render() {
    const { rangeParams, rangeType, query, id } = this.props.query.toObject();
    const { selectedView } = this.state.currentView;

    return (
      <Row className="no-bm">
        <Col md={12}
             id="universalsearch-container"
             style={{ paddingLeft: '15px', paddingRight: '15px', paddingBottom: '10px' }}>
          <Row className="no-bm">
            <Col md={12} id="universalsearch" style={{ marginTop: '0px', marginLeft: '15px', marginRight: '15px' }}>
              <form className="universalsearch-form"
                    method="GET"
                    onSubmit={this._performSearch}>
                <Row className="no-bm">
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

              </form>
            </Col>
          </Row>
        </Col>
      </Row>
    );
  },
});

export default SearchBar;
