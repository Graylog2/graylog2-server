import React from 'react';
import Reflux from 'reflux';
import { ButtonToolbar, Col, DropdownButton, MenuItem, Row } from 'react-bootstrap';
import Immutable from 'immutable';

import DocumentationLink from 'components/support/DocumentationLink';
import RefreshControls from 'components/search/RefreshControls';
import Input from 'components/bootstrap/Input';
import DocsHelper from 'util/DocsHelper';
import SearchButton from 'enterprise/components/searchbar/SearchButton';
import SearchStore from 'enterprise/stores/SearchStore';
import SearchActions from 'enterprise/actions/SearchActions';
import RelativeTimeRangeSelector from 'enterprise/components/searchbar/RelativeTimeRangeSelector';
import AbsoluteTimeRangeSelector from 'enterprise/components/searchbar/AbsoluteTimeRangeSelector';
import KeywordTimeRangeSelector from 'enterprise/components/searchbar/KeywordTimeRangeSelector';

const SearchBar = React.createClass({
  mixins: [Reflux.connect(SearchStore, 'search')],
  _performSearch(event) {
    event.preventDefault();

  },
  // TODO: Transfer this to AbsoluteTimeRangeSelector
  _rangeParamsChanged(key, value) {
    return () => {
      let refInput;

      switch (key) {
        case 'from':
        case 'to':
          const ref = `${key}Formatted`;
          refInput = this.refs[ref];
          if (!this._isValidDateString(refInput.getValue())) {
            refInput.getInputDOMNode().setCustomValidity('Invalid date time provided');
          } else {
            refInput.getInputDOMNode().setCustomValidity('');
          }
          break;
        default:
          refInput = this.refs[key];
      }
      SearchActions.rangeParams(key, refInput.getValue);
    };
  },
  _rangeTypeChanged(value) {
    SearchActions.rangeType(value);
  },
  _getRangeTypeSelector(rangeType, rangeParams) {
    const onChange = SearchActions.rangeParams;
    switch (rangeType) {
      case 'relative':
        return <RelativeTimeRangeSelector value={rangeParams} config={this.props.config} onChange={onChange}/>;
      case 'absolute':
        return <AbsoluteTimeRangeSelector value={rangeParams} onChange={onChange} />;
      case 'keyword':
        return <KeywordTimeRangeSelector value={rangeParams} onChange={onChange} />;
      default:
        throw new Error(`Unsupported range type ${rangeType}`);
    }
  },
  _getSavedSearchesSelector() {},
  _queryChanged() {},
  getInitialState() {
    return {
      savedSearch: '',
      keywordPreview: Immutable.Map(),
    };
  },
  render() {
    const { rangeParams, rangeType, query } = this.state.search;
    return (
      <Row className="no-bm">
        <Col md={12} id="universalsearch-container">
          <Row className="no-bm">
            <Col md={12} ref="universalSearch" id="universalsearch">
              <form ref="searchForm"
                className="universalsearch-form"
                method="GET"
                onSubmit={this._performSearch}>
                <input type="hidden" name="rangetype" value={rangeType} />
                <input type="hidden" ref={(ref) => { this.fields = ref; }} name="fields" value="" />
                <input type="hidden" ref={(ref) => { this.width = ref; }} name="width" value="" />
                <input type="hidden" ref={(ref) => { this.highlightMessage = ref; }} name="highlightMessage" value="" />

                <div className="timerange-selector-container">
                  <div className="row no-bm">
                    <div className="col-md-6">
                      <ButtonToolbar className="timerange-chooser pull-left">
                        <DropdownButton bsStyle="info"
                          title={<i className="fa fa-clock-o" />}
                          onSelect={this._rangeTypeChanged}
                          id="dropdown-timerange-selector">
                          <MenuItem eventKey="relative"
                            className={rangeType === 'relative' ? 'selected' : null}>
                            Relative
                          </MenuItem>
                          <MenuItem eventKey="absolute"
                            className={rangeType === 'absolute' ? 'selected' : null}>
                            Absolute
                          </MenuItem>
                          <MenuItem eventKey="keyword"
                            className={rangeType === 'keyword' ? 'selected' : null}>
                            Keyword
                          </MenuItem>
                        </DropdownButton>
                      </ButtonToolbar>

                      {this._getRangeTypeSelector(rangeType, rangeParams)}
                    </div>
                    <div className="col-md-6">
                      <div className="saved-searches-selector-container pull-right"
                        style={{ display: 'inline-flex', marginRight: 5 }}>
                        {this.props.displayRefreshControls &&
                        <div style={{ marginRight: 5 }}>
                          <RefreshControls />
                        </div>
                        }
                        <div style={{ width: 270 }}>
                          {this._getSavedSearchesSelector()}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <div id="search-container">
                  <div className="pull-right search-help">
                    <DocumentationLink page={DocsHelper.PAGES.SEARCH_QUERY_LANGUAGE}
                      title="Search query syntax documentation"
                      text={<i className="fa fa-lightbulb-o" />} />
                  </div>

                  <SearchButton />

                  <div className="query">
                    <Input type="text"
                      ref="query"
                      name="q"
                      value={query}
                      onChange={SearchActions.query}
                      placeholder="Type your search query here and press enter. (&quot;not found&quot; AND http) OR http_response_code:[400 TO 404]" />
                  </div>
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
