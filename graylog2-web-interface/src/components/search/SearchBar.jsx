/* eslint-disable */
import $ from 'jquery';
import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import ReactDOM from 'react-dom';
import Immutable from 'immutable';
import URI from 'urijs';

import { Button, ButtonToolbar, DropdownButton, MenuItem, Alert } from 'components/graylog';
import { Input } from 'components/bootstrap';
import { DatePicker, Select, Icon } from 'components/common';
import { RefreshControls, QueryInput } from 'components/search';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';

import StoreProvider from 'injection/StoreProvider';

import ActionsProvider from 'injection/ActionsProvider';

import UIUtils from 'util/UIUtils';

import DateTime from 'logic/datetimes/DateTime';
import moment from 'moment';

const SearchStore = StoreProvider.getStore('Search');
const ToolsStore = StoreProvider.getStore('Tools');
const SavedSearchesActions = ActionsProvider.getActions('SavedSearches');

const SearchBar = createReactClass({
  displayName: 'SearchBar',

  propTypes: {
    userPreferences: PropTypes.object,
    savedSearches: PropTypes.arrayOf(PropTypes.object).isRequired,
    config: PropTypes.object,
    displayRefreshControls: PropTypes.bool,
    onExecuteSearch: PropTypes.func,
  },

  getDefaultProps() {
    return {
      displayRefreshControls: true,
    };
  },

  getInitialState() {
    this.initialSearchParams = SearchStore.getParams();
    return {
      rangeType: this.initialSearchParams.rangeType,
      rangeParams: this.initialSearchParams.rangeParams,
      query: this.initialSearchParams.query,
      savedSearch: SearchStore.savedSearch,
      keywordPreview: Immutable.Map(),
    };
  },

  componentDidMount() {
    SearchStore.onParamsChanged = newParams => this.setState(newParams);
    SearchStore.onSubmitSearch = () => {
      this._performSearch();
    };
    SearchStore.onAddQueryTerm = this._animateQueryChange;
    this._initializeSearchQueryInput();
  },

  componentDidUpdate(prevProps, prevState) {
    if (this.state.query !== prevState.query) {
      this._updateSearchQueryInput(this.state.query);
    }
  },

  componentWillUnmount() {
    this._removeSearchQueryInput();
  },

  inputs: {},

  reload() {
    this.setState(this.getInitialState());
  },

  _initializeSearchQueryInput() {
    if (this.props.userPreferences.enableSmartSearch) {
      this.queryInput = new QueryInput(this.query.getInputDOMNode());
      this.queryInput.display();
      // We need to update on changes made on typeahead
      const queryDOMElement = ReactDOM.findDOMNode(this.query);
      $(queryDOMElement).on('typeahead:change', (event) => {
        SearchStore.query = event.target.value;
      });
    }
  },

  _updateSearchQueryInput(value) {
    if (this.props.userPreferences.enableSmartSearch) {
      this.queryInput.update(value);
    }
  },

  _removeSearchQueryInput() {
    if (this.props.userPreferences.enableSmartSearch) {
      const queryDOMElement = ReactDOM.findDOMNode(this.query);
      $(queryDOMElement).off('typeahead:change');
    }
  },

  _closeSearchQueryAutoCompletion() {
    if (this.props.userPreferences.enableSmartSearch) {
      const queryDOMElement = ReactDOM.findDOMNode(this.query.getInputDOMNode());
      $(queryDOMElement).typeahead('close');
    }
  },

  _animateQueryChange() {
    UIUtils.scrollToHint(ReactDOM.findDOMNode(this.universalSearch));
    $(ReactDOM.findDOMNode(this.query)).effect('bounce');
  },

  _queryChanged() {
    SearchStore.query = this.query.getValue();
  },

  _rangeTypeChanged(newRangeType, event) {
    SearchStore.rangeType = newRangeType;
    this._resetKeywordPreview();
  },

  _rangeParamsChanged(key) {
    return () => {
      let refInput;

      /* eslint-disable no-case-declarations */
      switch (key) {
        case 'from':
        case 'to':
          const ref = `${key}Formatted`;
          refInput = this.inputs[ref];
          if (!this._isValidDateString(refInput.getValue())) {
            refInput.getInputDOMNode().setCustomValidity('Invalid date time provided');
          } else {
            refInput.getInputDOMNode().setCustomValidity('');
          }
          break;
        default:
          refInput = this.inputs[key];
      }
      /* eslint-enable no-case-declarations */
      SearchStore.rangeParams = this.state.rangeParams.set(key, refInput.getValue());
    };
  },

  _keywordSearchChanged() {
    this._rangeParamsChanged('keyword')();
    const value = this.inputs.keyword.getValue();

    if (value === '') {
      this._resetKeywordPreview();
    } else {
      ToolsStore.testNaturalDate(value)
        .then(data => this._onKeywordPreviewLoaded(data))
        .catch(() => this._resetKeywordPreview());
    }
  },

  _resetKeywordPreview() {
    this.setState({ keywordPreview: Immutable.Map() });
  },

  _onKeywordPreviewLoaded(data) {
    const from = DateTime.fromUTCDateTime(data.from).toString();
    const to = DateTime.fromUTCDateTime(data.to).toString();
    this.setState({ keywordPreview: Immutable.Map({ from: from, to: to }) });
  },

  _formattedDateStringInUserTZ(field) {
    const dateString = this.state.rangeParams.get(field);

    if (dateString === null || dateString === undefined || dateString === '') {
      return dateString;
    }

    // We only format the original dateTime, as datepicker will format the date in another way, and we
    // don't want to annoy users trying to guess what they are typing.
    if (this.initialSearchParams.rangeParams.get(field) === dateString) {
      return DateTime.parseFromString(dateString).toString();
    }

    return dateString;
  },

  _setDateTimeToNow(field) {
    return () => {
      const inputNode = this.inputs[`${field}Formatted`].getInputDOMNode();
      inputNode.value = new DateTime().toString(DateTime.Formats.DATETIME);
      this._rangeParamsChanged(field)();
    };
  },

  _isValidDateField(field) {
    return this._isValidDateString(this._formattedDateStringInUserTZ(field));
  },

  _isValidDateString(dateString) {
    try {
      if (dateString !== undefined) {
        DateTime.parseFromString(dateString);
      }
      return true;
    } catch (e) {
      return false;
    }
  },

  _performSearch(event) {
    if (event) {
      event.preventDefault();
    }

    this._closeSearchQueryAutoCompletion();

    // Convert from and to values to UTC
    if (this.state.rangeType === 'absolute') {
      const fromInput = this.inputs.fromFormatted.getValue();
      const toInput = this.inputs.toFormatted.getValue();

      this.from.value = DateTime.parseFromString(fromInput).toISOString();
      this.to.value = DateTime.parseFromString(toInput).toISOString();
    }

    this.fields.value = SearchStore.fields.join(',');
    this.width.value = SearchStore.width;
    this.highlightMessage.value = SearchStore.highlightMessage;

    const { searchForm } = this;
    const searchQuery = $(searchForm).serialize();
    const searchURI = new URI(searchForm.action).search(searchQuery);
    const resource = searchURI.resource();

    SearchStore.executeSearch(resource);
    if (typeof this.props.onExecuteSearch === 'function') {
      this.props.onExecuteSearch(resource);
    }
  },

  _onSavedSearchSelect(searchId) {
    if (searchId === '') {
      this._performSearch();
    }
    const streamId = SearchStore.searchInStream ? SearchStore.searchInStream.id : undefined;
    SavedSearchesActions.execute.triggerPromise(searchId, streamId, $(window).width());
  },

  _onDateSelected(field) {
    return (date, _, event) => {
      const inputField = this.inputs[`${field}Formatted`].getInputDOMNode();
      const midnightDate = date.setHours(0);
      inputField.value = DateTime.ignoreTZ(midnightDate).toString(DateTime.Formats.DATETIME);
      this._rangeParamsChanged(field)();
    };
  },

  _getRangeTypeSelector() {
    let selector;

    switch (this.state.rangeType) {
      case 'relative': {
        const availableOptions = this.props.config ? this.props.config.relative_timerange_options : null;
        const timeRangeLimit = this.props.config ? moment.duration(this.props.config.query_time_range_limit) : null;
        let options;

        if (availableOptions) {
          let all = null;
          options = Object.keys(availableOptions).map((key) => {
            const seconds = moment.duration(key).asSeconds();

            if (timeRangeLimit > 0 && (seconds > timeRangeLimit.asSeconds() || seconds === 0)) {
              return null;
            }

            const option = (<option key={`relative-option-${key}`} value={seconds}>{availableOptions[key]}</option>);

            // The "search in all messages" option should be the last one.
            if (key === 'PT0S') {
              all = option;
              return null;
            }
            return option;
          });

          if (all) {
            options.push(all);
          }
        } else {
          options = (<option value="300">Loading...</option>);
        }

        selector = (
          <div className="timerange-selector relative"
               style={{ width: 270, marginLeft: 50 }}>
            <Input id="relative-timerange-selector"
                   ref={(relative) => { this.inputs.relative = relative; }}
                   type="select"
                   value={this.state.rangeParams.get('relative')}
                   name="relative"
                   onChange={this._rangeParamsChanged('relative')}
                   className="input-sm">
              {options}
            </Input>
          </div>
        );
        break;
      }
      case 'absolute': {
        selector = (
          <div className="timerange-selector absolute" style={{ width: 600 }}>
            <div className="row no-bm" style={{ marginLeft: 50 }}>
              <div className="col-md-5" style={{ padding: 0 }}>
                <input type="hidden" name="from" ref={(ref) => { this.from = ref; }} />
                <DatePicker id="searchFromDatePicker"
                            title="Search start date"
                            date={this.state.rangeParams.get('from')}
                            onChange={this._onDateSelected('from')}>
                  <Input type="text"
                         ref={(fromFormatted) => { this.inputs.fromFormatted = fromFormatted; }}
                         id="timerange-absolute-from"
                         value={this._formattedDateStringInUserTZ('from')}
                         onChange={this._rangeParamsChanged('from')}
                         placeholder={DateTime.Formats.DATETIME}
                         buttonAfter={<Button bsSize="small" onClick={this._setDateTimeToNow('from')}><Icon name="magic" /></Button>}
                         bsStyle={this._isValidDateField('from') ? null : 'error'}
                         bsSize="small"
                         required />
                </DatePicker>

              </div>
              <div className="col-md-1">
                <p className="text-center" style={{ margin: 0, lineHeight: '30px' }}>to</p>
              </div>
              <div className="col-md-5" style={{ padding: 0 }}>
                <input type="hidden" name="to" ref={(ref) => { this.to = ref; }} />
                <DatePicker id="searchToDatePicker"
                            title="Search end date"
                            date={this.state.rangeParams.get('to')}
                            onChange={this._onDateSelected('to')}>
                  <Input type="text"
                         ref={(toFormatted) => { this.inputs.toFormatted = toFormatted; }}
                         id="timerange-absolute-to"
                         value={this._formattedDateStringInUserTZ('to')}
                         onChange={this._rangeParamsChanged('to')}
                         placeholder={DateTime.Formats.DATETIME}
                         buttonAfter={<Button bsSize="small" onClick={this._setDateTimeToNow('to')}><Icon name="magic" /></Button>}
                         bsStyle={this._isValidDateField('to') ? null : 'error'}
                         bsSize="small"
                         required />
                </DatePicker>
              </div>
            </div>
          </div>
        );
        break;
      }
      case 'keyword': {
        selector = (
          <div className="timerange-selector keyword" style={{ width: 650 }}>
            <div className="row no-bm" style={{ marginLeft: 50 }}>
              <div className="col-md-5" style={{ padding: 0 }}>
                <Input type="text"
                       ref={(keyword) => { this.inputs.keyword = keyword; }}
                       id="timerange-keyword"
                       name="keyword"
                       value={this.state.rangeParams.get('keyword')}
                       onChange={this._keywordSearchChanged}
                       placeholder="Last week"
                       className="input-sm"
                       required />
              </div>
              <div className="col-md-7" style={{ paddingRight: 0 }}>
                {this.state.keywordPreview.size > 0
                && (
                <Alert bsStyle="info" style={{ height: 30, paddingTop: 5, paddingBottom: 5, marginTop: 0 }}>
                  <strong style={{ marginRight: 8 }}>Preview:</strong>
                  {this.state.keywordPreview.get('from')} to {this.state.keywordPreview.get('to')}
                </Alert>
                )
                }
              </div>
            </div>
          </div>
        );
        break;
      }
      default:
        throw new Error(`Unsupported range type ${this.state.rangeType}`);
    }

    return selector;
  },

  _getSavedSearchesSelector() {
    const formattedSavedSearches = this.props.savedSearches
      .sort((searchA, searchB) => searchA.title.toLowerCase().localeCompare(searchB.title.toLowerCase()))
      .map((savedSearch) => {
        return { value: savedSearch.id, label: savedSearch.title };
      });

    return (
      <Select placeholder="Saved searches"
              options={formattedSavedSearches}
              value={this.state.savedSearch}
              onChange={this._onSavedSearchSelect}
              size="small" />
    );
  },

  render() {
    return (
      <div className="row no-bm">
        <div className="col-md-12" id="universalsearch-container">
          <div className="row no-bm">
            <div ref={(universalSearch) => { this.universalSearch = universalSearch; }} className="col-md-12" id="universalsearch">
              <form ref={(searchForm) => { this.searchForm = searchForm; }}
                    className="universalsearch-form"
                    action={SearchStore.searchBaseLocation('index')}
                    method="GET"
                    onSubmit={this._performSearch}>
                {this.state.savedSearch && <input type="hidden" name="saved" value={this.state.savedSearch} />}
                <input type="hidden" name="rangetype" value={this.state.rangeType} />
                <input type="hidden" ref={(ref) => { this.fields = ref; }} name="fields" value="" />
                <input type="hidden" ref={(ref) => { this.width = ref; }} name="width" value="" />
                <input type="hidden" ref={(ref) => { this.highlightMessage = ref; }} name="highlightMessage" value="" />

                <div className="timerange-selector-container">
                  <div className="row no-bm">
                    <div className="col-md-6">
                      <ButtonToolbar className="timerange-chooser pull-left">
                        <DropdownButton bsStyle="info"
                                        title={<Icon name="clock-o" />}
                                        onSelect={this._rangeTypeChanged}
                                        id="dropdown-timerange-selector">
                          <MenuItem eventKey="relative"
                                    active={this.state.rangeType === 'relative'}>
                            Relative
                          </MenuItem>
                          <MenuItem eventKey="absolute"
                                    active={this.state.rangeType === 'absolute'}>
                            Absolute
                          </MenuItem>
                          <MenuItem eventKey="keyword"
                                    active={this.state.rangeType === 'keyword'}>
                            Keyword
                          </MenuItem>
                        </DropdownButton>
                      </ButtonToolbar>

                      {this._getRangeTypeSelector()}
                    </div>
                    <div className="col-md-6">
                      <div className="saved-searches-selector-container pull-right"
                           style={{ display: 'inline-flex', marginRight: 5 }}>
                        {this.props.displayRefreshControls
                        && (
                        <div style={{ marginRight: 5 }}>
                          <RefreshControls />
                        </div>
                        )
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
                                       text={<Icon name="lightbulb-o" />} />
                  </div>

                  <Button type="submit" bsStyle="success" className="pull-left">
                    <Icon name="search" />
                  </Button>

                  <div className="query">
                    <Input type="text"
                           ref={(query) => { this.query = query; }}
                           id="search-field"
                           name="q"
                           value={this.state.query}
                           onChange={this._queryChanged}
                           placeholder="Type your search query here and press enter. (&quot;not found&quot; AND http) OR http_response_code:[400 TO 404]" />
                  </div>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    );
  },
});

export default SearchBar;
