/* global momentHelper, userPreferences */

import $ from 'jquery';

import React from 'react';
import Immutable from 'immutable';
import { Input, Button, ButtonToolbar, DropdownButton, MenuItem, Alert } from 'react-bootstrap';

import ChosenSelectInput from 'components/common/ChosenSelectInput';
import QueryInput from './QueryInput';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';

import SearchStore from 'stores/search/SearchStore';
import SavedSearchesStore from 'stores/search/SavedSearchesStore';
import ToolsStore from 'stores/tools/ToolsStore';

import UIUtils from 'util/UIUtils';

const SearchBar = React.createClass({
  getInitialState() {
    this.initialSearchParams = SearchStore.getParams();
    this.datepickerInitialized = false;
    return {
      rangeType: this.initialSearchParams.rangeType,
      rangeParams: this.initialSearchParams.rangeParams,
      query: this.initialSearchParams.query,
      savedSearch: SearchStore.savedSearch,
      savedSearches: Immutable.List(),
      keywordPreview: Immutable.Map(),
    };
  },
  componentDidMount() {
    SearchStore.onParamsChanged = (newParams) => this.setState(newParams);
    SearchStore.onSubmitSearch = () => {
      this._prepareSearch();
      React.findDOMNode(this.refs.searchForm).submit();
    };
    SearchStore.onAddQueryTerm = this._animateQueryChange;
    SavedSearchesStore.addOnSavedSearchesChangedListener((newSavedSearches) => this.setState({savedSearches: newSavedSearches}));
    this._initializeSearchQueryInput();
    this._initalizeDatepicker();
  },
  componentDidUpdate() {
    this._initalizeDatepicker();
  },
  componentWillUnmount() {
    this._removeSearchQueryInput();
  },
  render() {
    return (
      <div className="row no-bm">
        <div className="col-md-12" id="universalsearch-container">
          <div className="row no-bm">
            <div ref="universalSearch" className="col-md-12" id="universalsearch">
              <form ref="searchForm"
                    className="universalsearch-form"
                    action={SearchStore.searchBaseLocation('index')}
                    method="GET"
                    onSubmit={this._prepareSearch}>
                <Input type="hidden" name="rangetype" value={this.state.rangeType}/>
                <Input type="hidden" ref="fields" name="fields" value=""/>
                <Input type="hidden" ref="width" name="width" value=""/>

                <div className="timerange-selector-container">
                  <div className="row no-bm">
                    <div className="col-md-9">
                      <ButtonToolbar className="timerange-chooser pull-left">
                        <DropdownButton bsStyle="info"
                                        title={<i className="fa fa-clock-o"></i>}
                                        onSelect={this._rangeTypeChanged}>
                          <MenuItem eventKey="relative"
                                    className={this.state.rangeType === 'relative' ? 'selected' : null}>
                            Relative
                          </MenuItem>
                          <MenuItem eventKey="absolute"
                                    className={this.state.rangeType === 'absolute' ? 'selected' : null}>
                            Absolute
                          </MenuItem>
                          <MenuItem eventKey="keyword"
                                    className={this.state.rangeType === 'keyword' ? 'selected' : null}>
                            Keyword
                          </MenuItem>
                        </DropdownButton>
                      </ButtonToolbar>

                      {this._getRangeTypeSelector()}
                    </div>
                    <div className="col-md-3">
                      <div className="saved-searches-selector-container"
                           style={{float: 'right', marginRight: 5, width: 270}}>
                        {this._getSavedSearchesSelector()}
                      </div>
                    </div>
                  </div>
                </div>

                <div id="search-container">
                  <div className="pull-right search-help">
                    <DocumentationLink page={DocsHelper.PAGES.SEARCH_QUERY_LANGUAGE}
                                       title="Search query syntax documentation"
                                       text={<i className="fa fa-lightbulb-o"></i>}/>
                  </div>

                  <Button type="submit" bsStyle="success" className="pull-left">
                    <i className="fa fa-search"></i>
                  </Button>

                  <div className="query">
                    <Input type="text"
                           ref="query"
                           name="q"
                           value={this.state.query}
                           onChange={this._queryChanged}
                           placeholder="Type your search query here and press enter. (&quot;not found&quot; AND http) OR http_response_code:[400 TO 404]"/>
                  </div>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    );
  },
  _initializeSearchQueryInput() {
    if (userPreferences.enableSmartSearch) {
      const queryInput = new QueryInput(this.refs.query.getInputDOMNode());
      queryInput.display();
      // We need to update on changes made on typeahead
      const queryDOMElement = React.findDOMNode(this.refs.query);
      $(queryDOMElement).on('typeahead:change', (event) => {
        SearchStore.query = event.target.value;
      });
    }
  },
  _removeSearchQueryInput() {
    if (userPreferences.enableSmartSearch) {
      const queryDOMElement = React.findDOMNode(this.refs.query);
      $(queryDOMElement).off('typeahead:change');
    }
  },
  // We need to initialize datepicker every time the absolute timerange is selected, but only once :/
  _initalizeDatepicker() {
    if (this.state.rangeType !== 'absolute') {
      this.datepickerInitialized = false;
      return;
    }

    if (this.datepickerInitialized) {
      return;
    }

    ['from', 'to'].forEach((field) => {
      const input = this.refs[field + 'Formatted'].getInputDOMNode();
      const that = this; // Thank you for this (or that) jquery

      $(input).datepicker({
        format: 'yyyy-mm-dd',
        weekStart: 1,
      }).on('changeDate', (event) => {
        const dateString = event.target.value + ' 00:00:00';
        const date = momentHelper.parseUserLocalFromString(dateString);
        event.target.value = date.format(momentHelper.DATE_FORMAT_TZ);
        that._rangeParamsChanged(field)();
      });
    });

    this.datepickerInitialized = true;
  },
  _animateQueryChange() {
    UIUtils.scrollToHint(React.findDOMNode(this.refs.universalSearch));
    $(React.findDOMNode(this.refs.query)).effect('bounce');
  },
  _queryChanged() {
    SearchStore.query = this.refs.query.getValue();
  },
  _rangeTypeChanged(newRangeType) {
    SearchStore.rangeType = newRangeType;
  },
  _rangeParamsChanged(key) {
    return () => {
      const ref = (key === 'from' || key === 'to') ? key + 'Formatted' : key;
      SearchStore.rangeParams = this.state.rangeParams.set(key, this.refs[ref].getValue());
    };
  },
  _keywordSearchChanged() {
    this._rangeParamsChanged('keyword')();
    const value = this.refs.keyword.getValue();

    if (value === '') {
      this._resetKeywordPreview();
    } else {
      const promise = ToolsStore.testNaturalDate(value);
      promise.fail(() => this._resetKeywordPreview());
      promise.done((data) => this._onKeywordPreviewLoaded(data));
    }
  },
  _resetKeywordPreview() {
    this.setState({keywordPreview: Immutable.Map()});
  },
  _onKeywordPreviewLoaded(data) {
    const from = momentHelper.toUserTimeZone(data.from).format(momentHelper.DATE_FORMAT_NO_MS);
    const to = momentHelper.toUserTimeZone(data.to).format(momentHelper.DATE_FORMAT_NO_MS);
    this.setState({keywordPreview: Immutable.Map({from: from, to: to})});
  },
  _formattedDateStringInUserTZ(field) {
    const dateString = this.state.rangeParams.get(field);

    if (dateString === null || dateString === undefined || dateString === '') {
      return dateString;
    }

    // We only format the original dateTime, as datepicker will format the date in another way, and we
    // don't want to annoy users trying to guess what they are typing.
    if (this.initialSearchParams.rangeParams.get(field) === dateString) {
      const originalDateTime = momentHelper.parseFromString(dateString);
      return momentHelper.toUserTimeZone(originalDateTime).format(momentHelper.DATE_FORMAT_TZ);
    }

    return dateString;
  },
  _setDateTimeToNow(field) {
    return () => {
      const inputNode = this.refs[field + 'Formatted'].getInputDOMNode();
      inputNode.value = momentHelper.toUserTimeZone().format(momentHelper.DATE_FORMAT_TZ);
      this._rangeParamsChanged(field)();
    };
  },
  _isValidDateField(field) {
    return this._isValidDateString(this._formattedDateStringInUserTZ(field));
  },
  _isValidDateString(dateString) {
    return dateString === undefined || momentHelper.parseFromString(dateString).isValid();
  },
  _prepareSearch() {
    // Convert from and to values to UTC
    if (this.state.rangeType === 'absolute') {
      const fromInput = this.refs.fromFormatted.getValue();
      const toInput = this.refs.toFormatted.getValue();

      const fromMoment = momentHelper.parseUserLocalFromString(fromInput);
      this.refs.from.getInputDOMNode().value = fromMoment.toISOString();

      const toMoment = momentHelper.parseUserLocalFromString(toInput);
      this.refs.to.getInputDOMNode().value = toMoment.toISOString();
    }

    this.refs.fields.getInputDOMNode().value = SearchStore.fields.join(',');
    this.refs.width.getInputDOMNode().value = SearchStore.width;
  },
  _savedSearchSelected() {
    const selectedSavedSearch = this.refs.savedSearchesSelector.getValue();
    const streamId = SearchStore.searchInStream ? SearchStore.searchInStream.id : undefined;
    SavedSearchesStore.execute(selectedSavedSearch, streamId, $(window).width());
  },

  _getRangeTypeSelector() {
    let selector;

    switch (this.state.rangeType) {
      case 'relative':
        selector = (
          <div className="timerange-selector relative"
               style={{width: 270, marginLeft: 50}}>
            <Input id="relative-timerange-selector"
                   ref="relative"
                   type="select"
                   value={this.state.rangeParams.get('relative')}
                   name="relative"
                   onChange={this._rangeParamsChanged('relative')}
                   className="input-sm">
              <option value="300">Search in the last 5 minutes</option>
              <option value="900">Search in the last 15 minutes</option>
              <option value="1800">Search in the last 30 minutes</option>
              <option value="3600">Search in the last 1 hour</option>
              <option value="7200">Search in the last 2 hours</option>
              <option value="28800">Search in the last 8 hours</option>
              <option value="86400">Search in the last 1 day</option>
              <option value="172800">Search in the last 2 days</option>
              <option value="432000">Search in the last 5 days</option>
              <option value="604800">Search in the last 7 days</option>
              <option value="1209600">Search in the last 14 days</option>
              <option value="2592000">Search in the last 30 days</option>
              <option value="0">Search in all messages</option>
            </Input>
          </div>
        );
        break;
      case 'absolute':
        selector = (
          <div className="timerange-selector absolute" style={{width: 600}}>
            <div className="row no-bm" style={{marginLeft: 50}}>
              <div className="col-md-5" style={{padding: 0}}>
                <Input type="hidden" name="from" ref="from"/>
                <Input type="text"
                       ref="fromFormatted"
                       value={this._formattedDateStringInUserTZ('from')}
                       onChange={this._rangeParamsChanged('from')}
                       placeholder={momentHelper.DATE_FORMAT}
                       buttonAfter={<Button bsSize="small" onClick={this._setDateTimeToNow('from')}><i className="fa fa-magic"></i></Button>}
                       bsStyle={this._isValidDateField('from') ? null : 'error'}
                       bsSize="small"
                       required/>
              </div>
              <div className="col-md-1">
                <p className="text-center" style={{margin: 0, lineHeight: '30px'}}>to</p>
              </div>
              <div className="col-md-5" style={{padding: 0}}>
                <Input type="hidden" name="to" ref="to"/>
                <Input type="text"
                       ref="toFormatted"
                       value={this._formattedDateStringInUserTZ('to')}
                       onChange={this._rangeParamsChanged('to')}
                       placeholder={momentHelper.DATE_FORMAT}
                       buttonAfter={<Button bsSize="small" onClick={this._setDateTimeToNow('to')}><i className="fa fa-magic"></i></Button>}
                       bsStyle={this._isValidDateField('to') ? null : 'error'}
                       bsSize="small"
                       required/>
              </div>
            </div>
          </div>
        );
        break;
      case 'keyword':
        selector = (
          <div className="timerange-selector keyword" style={{width: 650}}>
            <div className="row no-bm" style={{marginLeft: 50}}>
              <div className="col-md-5" style={{padding: 0}}>
                <Input type="text"
                       ref="keyword"
                       name="keyword"
                       value={this.state.rangeParams.get('keyword')}
                       onChange={this._keywordSearchChanged}
                       placeholder="Last week"
                       className="input-sm"
                       required/>
              </div>
              <div className="col-md-7" style={{paddingRight: 0}}>
                {this.state.keywordPreview.size > 0 &&
                <Alert bsStyle="info" style={{height: 30, paddingTop: 5, paddingBottom: 5, marginTop: 0}}>
                  <strong style={{marginRight: 8}}>Preview:</strong>
                  {this.state.keywordPreview.get('from')} to {this.state.keywordPreview.get('to')}
                </Alert>
                }
              </div>
            </div>
          </div>
        );
        break;
      default:
        throw ('Unsupported range type ' + this.state.rangeType);
    }

    return selector;
  },

  _getSavedSearchesSelector() {
    const sortedSavedSearches = this.state.savedSearches.sort((a, b) => {
      return a.title.toLowerCase().localeCompare(b.title.toLowerCase());
    });

    return (
      <ChosenSelectInput ref="savedSearchesSelector"
                         className="input-sm"
                         value={this.state.savedSearch}
                         dataPlaceholder="Saved searches"
                         onChange={this._savedSearchSelected}>
        {sortedSavedSearches.map((savedSearch) => {
          return <option key={savedSearch.id} value={savedSearch.id}>{savedSearch.title}</option>;
        })}
      </ChosenSelectInput>
    );
  },
});

export default SearchBar;
