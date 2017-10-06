import React from 'react';
import Reflux from 'reflux';
import { Col, Row } from 'react-bootstrap';
import Immutable from 'immutable';

import DocumentationLink from 'components/support/DocumentationLink';
import RefreshControls from 'components/search/RefreshControls';
import DocsHelper from 'util/DocsHelper';

import SearchButton from 'enterprise/components/searchbar/SearchButton';
import SearchStore from 'enterprise/stores/SearchStore';
import SearchActions from 'enterprise/actions/SearchActions';
import TimeRangeInput from 'enterprise/components/searchbar/TimeRangeSelector';
import TimeRangeTypeSelector from 'enterprise/components/searchbar/TimeRangeTypeSelector';
import QueryInput from 'enterprise/components/searchbar/QueryInput';

const SearchBar = React.createClass({
  mixins: [Reflux.connect(SearchStore, 'search')],
  getInitialState() {
    return {
      savedSearch: '',
      keywordPreview: Immutable.Map(),
    };
  },
  _performSearch(event) {
    event.preventDefault();
    const { search } = this.state;
    this.props.onExecute(search.fullQuery);
  },
  _getSavedSearchesSelector() {
  },
  render() {
    const { rangeParams, rangeType, query } = this.state.search;
    return (
      <Row className="no-bm">
        <Col md={12} id="universalsearch-container"
             style={{ paddingLeft: '15px', paddingRight: '15px', paddingBottom: '10px' }}>
          <Row className="no-bm">
            <Col md={12} id="universalsearch" style={{ marginTop: '0px' }}>
              <form className="universalsearch-form"
                    method="GET"
                    onSubmit={this._performSearch}>
                <div className="timerange-selector-container">
                  <Row className="no-bm">
                    <Col md={6}>
                      <TimeRangeTypeSelector onSelect={SearchActions.rangeType} value={rangeType}/>
                      <TimeRangeInput onChange={SearchActions.rangeParams}
                                      rangeType={rangeType}
                                      rangeParams={rangeParams}
                                      config={this.props.config}/>
                    </Col>
                    <Col md={6}>
                      <div className="saved-searches-selector-container pull-right"
                           style={{ display: 'inline-flex', marginRight: 5 }}>
                        {this.props.displayRefreshControls &&
                        <div style={{ marginRight: 5 }}>
                          <RefreshControls/>
                        </div>
                        }
                        <div style={{ width: 270 }}>
                          {this._getSavedSearchesSelector()}
                        </div>
                      </div>
                    </Col>
                  </Row>
                </div>

                <div id="search-container">
                  <div className="pull-right search-help">
                    <DocumentationLink page={DocsHelper.PAGES.SEARCH_QUERY_LANGUAGE}
                                       title="Search query syntax documentation"
                                       text={<i className="fa fa-lightbulb-o"/>}/>
                  </div>

                  <SearchButton/>

                  <QueryInput value={query} onChange={SearchActions.query}/>
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
