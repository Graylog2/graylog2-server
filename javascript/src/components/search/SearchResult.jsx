import $ from 'jquery';
import React, { PropTypes } from 'react';
import ReactDOM from 'react-dom';
import Immutable from 'immutable';

import SearchSidebar from './SearchSidebar';
import ResultTable from './ResultTable';
import { LegacyHistogram } from 'components/search';
import FieldGraphs from './FieldGraphs';
import FieldQuickValues from './FieldQuickValues';
import FieldStatistics from './FieldStatistics';
import ShowQueryModal from './ShowQueryModal';

import AddToDashboardMenu from 'components/dashboard/AddToDashboardMenu';
import Widget from 'components/widgets/Widget';
import DocumentationLink from 'components/support/DocumentationLink';

import DashboardStore from 'stores/dashboard/DashboardStore';
import SearchStore from 'stores/search/SearchStore';
import DocsHelper from 'util/DocsHelper';

let resizeMutex;

const SearchResult = React.createClass({
  propTypes: {
    query: PropTypes.string,
    builtQuery: PropTypes.string,
    result: PropTypes.object.isRequired,
    histogram: PropTypes.object.isRequired,
    formattedHistogram: PropTypes.array,
    searchInStream: PropTypes.object,
    streams: PropTypes.instanceOf(Immutable.Map),
    inputs: PropTypes.instanceOf(Immutable.Map),
    nodes: PropTypes.instanceOf(Immutable.Map),
    permissions: PropTypes.array.isRequired,
  },

  getInitialState() {
    const initialFields = SearchStore.fields;
    return {
      selectedFields: this.sortFields(initialFields),
      sortField: SearchStore.sortField,
      sortOrder: SearchStore.sortOrder,
      showAllFields: false,
      currentSidebarWidth: null,
      shouldHighlight: true,
      currentPage: SearchStore.page,
    };
  },

  getDefaultProps() {
    return {
      query: '*',
      builtQuery: '',
      formattedHistogram: [],
      searchInStream: null,
      streams: Immutable.Map({}),
      inputs: Immutable.Map({}),
      nodes: Immutable.Map({}),
    };
  },

  componentDidMount() {
    this._updateWidth();
    this._initializeAffix();
    DashboardStore.updateWritableDashboards();
    $(window).on('resize', this._resizeCallback);
  },

  componentWillUnmount() {
    $(window).off('resize', this._resizeCallback);
  },

  onFieldToggled(fieldName) {
    const currentFields = this.state.selectedFields;
    let newFieldSet;
    if (currentFields.contains(fieldName)) {
      newFieldSet = currentFields.delete(fieldName);
    } else {
      newFieldSet = currentFields.add(fieldName);
    }
    this.updateSelectedFields(newFieldSet);
  },

  togglePageFields() {
    this.setState({showAllFields: !this.state.showAllFields});
  },

  predefinedFieldSelection(setName) {
    if (setName === 'none') {
      this.updateSelectedFields(Immutable.Set());
    } else if (setName === 'all') {
      this.updateSelectedFields(Immutable.Set(this._fields().map(field => field.name)));
    } else if (setName === 'default') {
      this.updateSelectedFields(Immutable.Set(['message', 'source']));
    }
  },

  updateSelectedFields(fieldSelection) {
    const selectedFields = this.sortFields(fieldSelection);
    SearchStore.fields = selectedFields;
    this.setState({selectedFields: selectedFields});
  },
  _fields() {
    return this.props.result[this.state.showAllFields ? 'all_fields' : 'fields'];
  },

  sortFields(fieldSet) {
    let newFieldSet = fieldSet;
    let sortedFields = Immutable.OrderedSet();

    if (newFieldSet.contains('source')) {
      sortedFields = sortedFields.add('source');
    }
    newFieldSet = newFieldSet.delete('source');
    const remainingFieldsSorted = newFieldSet.sort((field1, field2) => field1.toLowerCase().localeCompare(field2.toLowerCase()));
    return sortedFields.concat(remainingFieldsSorted);
  },

  addFieldGraph(field) {
    this.refs.fieldGraphsComponent.addFieldGraph(field);
  },
  addFieldQuickValues(field) {
    this.refs.fieldQuickValuesComponent.addFieldQuickValues(field);
  },
  addFieldStatistics(field) {
    this.refs.fieldStatisticsComponent.addFieldStatistics(field);
  },

  _initializeAffix() {
    $(ReactDOM.findDOMNode(this.refs.oma)).affix({
      offset: {top: 111},
    });
  },
  _resizeCallback() {
    // Call resizedWindow() only at end of resize event so we do not trigger all the time while resizing.
    clearTimeout(resizeMutex);
    resizeMutex = setTimeout(() => this._updateWidth(), 100);
  },
  _updateWidth() {
    const node = ReactDOM.findDOMNode(this.refs.opa);
    this.setState({currentSidebarWidth: $(node).width()});
  },
  _showQueryModal(event) {
    event.preventDefault();
    this.refs.showQueryModal.open();
  },

  render() {
    let style = {};
    if (this.state.currentSidebarWidth) {
      style = {width: this.state.currentSidebarWidth};
    }
    const anyHighlightRanges = Immutable.fromJS(this.props.result.messages).some(message => message.get('highlight_ranges') !== null);

    // short circuit if the result turned up empty
    if (this.props.result.total_result_count === 0) {
      let streamDescription = null;
      if (this.props.searchInStream) {
        streamDescription = 'in stream ' + this.props.searchInStream.title;
      }
      return (
        <div>
          <div className="row content content-head">
            <div className="col-md-12">
              <h1>
                <span className="pull-right">
                  <AddToDashboardMenu title="Add count to dashboard"
                                      pullRight
                                      widgetType={this.props.searchInStream ? Widget.Type.STREAM_SEARCH_RESULT_COUNT : Widget.Type.SEARCH_RESULT_COUNT}
                                      permissions={this.props.permissions}/>
                </span>
                <span>Nothing found {streamDescription}</span>
              </h1>

              <p className="description">
                Your search returned no results.&nbsp;
                <a href="#" onClick={this._showQueryModal}>Show the Elasticsearch query.</a>
                <ShowQueryModal key="debugQuery" ref="showQueryModal" builtQuery={this.props.builtQuery}/>
                <strong>&nbsp;Take a look at the&nbsp;<DocumentationLink page={DocsHelper.PAGES.SEARCH_QUERY_LANGUAGE}
                                                                         text="documentation"/>
                  &nbsp;if you need help with the search syntax.</strong>
              </p>
            </div>
          </div>
          <div className="row content">
            <div className="col-md-12">
              <div className="support-sources">
                <h2>Need help?</h2>
                Do not hesitate to consult the Graylog community if your questions are not answered in the&nbsp;
                <DocumentationLink page={DocsHelper.PAGES.WELCOME} text="documentation"/>.

                <ul>
                  <li><i className="fa fa-group"></i> <a href="https://www.graylog.org/community-support/"
                                                         target="_blank">Forum / Mailing list</a></li>
                  <li><i className="fa fa-github-alt"></i> <a
                    href="https://github.com/Graylog2/graylog2-web-interface/issues" target="_blank">Issue tracker</a>
                  </li>
                  <li><i className="fa fa-heart"></i> <a href="https://www.graylog.com/support/" target="_blank">Commercial
                    support</a></li>
                </ul>
              </div>

            </div>
          </div>
        </div>);
    }
    return (
      <div id="main-content-search" className="row">
        <div ref="opa" className="col-md-3 col-sm-12" id="sidebar">
          <div ref="oma" id="sidebar-affix" style={style}>
            <SearchSidebar result={this.props.result}
                           builtQuery={this.props.builtQuery}
                           selectedFields={this.state.selectedFields}
                           fields={this._fields()}
                           showAllFields={this.state.showAllFields}
                           togglePageFields={this.togglePageFields}
                           onFieldToggled={this.onFieldToggled}
                           onFieldSelectedForGraph={this.addFieldGraph}
                           onFieldSelectedForQuickValues={this.addFieldQuickValues}
                           onFieldSelectedForStats={this.addFieldStatistics}
                           predefinedFieldSelection={this.predefinedFieldSelection}
                           showHighlightToggle={anyHighlightRanges}
                           shouldHighlight={this.state.shouldHighlight}
                           toggleShouldHighlight={() => this.setState({shouldHighlight: !this.state.shouldHighlight})}
                           currentSavedSearch={SearchStore.savedSearch}
                           searchInStream={this.props.searchInStream}
                           permissions={this.props.permissions}
            />
          </div>
        </div>
        <div className="col-md-9 col-sm-12" id="main-content-sidebar">
          <FieldStatistics ref="fieldStatisticsComponent"
                           permissions={this.props.permissions}/>

          <FieldQuickValues ref="fieldQuickValuesComponent"
                            permissions={this.props.permissions}/>

          <LegacyHistogram formattedHistogram={this.props.formattedHistogram}
                           histogram={this.props.histogram}
                           permissions={this.props.permissions}
                           isStreamSearch={this.props.searchInStream !== null}/>

          <FieldGraphs ref="fieldGraphsComponent"
                       resolution={this.props.histogram.interval}
                       from={this.props.histogram.histogram_boundaries.from}
                       to={this.props.histogram.histogram_boundaries.to}
                       permissions={this.props.permissions}
                       searchInStream={this.props.searchInStream}/>

          <ResultTable messages={this.props.result.messages}
                       page={this.state.currentPage}
                       selectedFields={this.state.selectedFields}
                       sortField={this.state.sortField}
                       sortOrder={this.state.sortOrder}
                       resultCount={this.props.result.total_result_count}
                       inputs={this.props.inputs}
                       streams={this.props.streams}
                       nodes={this.props.nodes}
                       highlight={this.state.shouldHighlight}
          />

        </div>
      </div>);
  },
});

export default SearchResult;
