'use strict';

var $ = require('jquery');

var React = require('react');
var SearchSidebar = require('./SearchSidebar');
var ResultTable = require('./ResultTable');
var LegacyHistogram = require('./LegacyHistogram');
var FieldGraphs = require('./FieldGraphs');
var FieldQuickValues = require('./FieldQuickValues');
var FieldStatistics = require('./FieldStatistics');
var ModalTrigger = require('react-bootstrap').ModalTrigger;
var ShowQueryModal = require('./ShowQueryModal');
var AddToDashboardMenu = require('../dashboard/AddToDashboardMenu');
var Widget = require('../widgets/Widget');
var Immutable = require('immutable');

var DashboardStore = require('../../stores/dashboard/DashboardStore');
var SearchStore = require('../../stores/search/SearchStore');

var resizeMutex;

var SearchResult = React.createClass({
    getInitialState() {
        var initialFields = SearchStore.fields;
        return {
            selectedFields: initialFields,
            sortField: SearchStore.sortField,
            sortOrder: SearchStore.sortOrder,
            showAllFields: false,
            currentSidebarWidth: null,
            shouldHighlight: true,
            currentPage: SearchStore.page
        };
    },

    predefinedFieldSelection(setName) {
        if (setName === "none") {
            this.updateSelectedFields(Immutable.Set());
        } else if (setName === "all") {
            this.updateSelectedFields(Immutable.Set(this._fields().map((f) => f.name)));
        } else if (setName === "default") {
            this.updateSelectedFields(Immutable.Set(['message', 'source']));
        }
    },

    updateSelectedFields(fieldSelection) {
        var selectedFields = this.sortFields(fieldSelection);
        SearchStore.fields = selectedFields;
        this.setState({selectedFields: selectedFields});
    },
    _fields() {
        return this.props.result[this.state.showAllFields ? 'all_fields' : 'page_fields'];
    },

    onFieldToggled(fieldName) {
        var currentFields = this.state.selectedFields;
        var newFieldSet;
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

    sortFields(fieldSet) {
        var sortedFields = Immutable.OrderedSet();
        if (fieldSet.contains('source')) {
            sortedFields = sortedFields.add('source');
        }
        fieldSet = fieldSet.delete('source');
        var remainingFieldsSorted = fieldSet.sort((a, b) => a.toLowerCase().localeCompare(b.toLowerCase()));
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

    componentDidMount() {
        this._updateWidth();
        DashboardStore.updateDashboards();
        $(window).on('resize', this._resizeCallback);
    },
    componentWillUnmount() {
        $(window).off("resize", this._resizeCallback);
    },
    _resizeCallback() {
        // Call resizedWindow() only at end of resize event so we do not trigger all the time while resizing.
        clearTimeout(resizeMutex);
        resizeMutex = setTimeout(() => this._updateWidth(), 100);
    },
    _updateWidth() {
        var node = React.findDOMNode(this.refs.opa);
        this.setState({currentSidebarWidth: $(node).width()});
    },

    render() {
        var style = {};
        if (this.state.currentSidebarWidth) {
            style = {width: this.state.currentSidebarWidth};
        }
        var anyHighlightRanges = Immutable.fromJS(this.props.result.messages).some(message => message.get('highlight_ranges') !== null);

        // short circuit if the result turned up empty
        if (this.props.result['total_result_count'] === 0) {
            var streamDescription = null;
            if (this.props.searchInStream) {
                streamDescription = "in stream " + this.props.searchInStream.title;
            }
            return (
                <div>
                    <div className="row content content-head">
                        <div className="col-md-12">
                            <h1>
                                <span><i className="fa fa-search"></i> Nothing found {streamDescription}</span>
                                <AddToDashboardMenu title="Add count to dashboard"
                                                    widgetType={this.props.searchInStream ? Widget.Type.STREAM_SEARCH_RESULT_COUNT : Widget.Type.SEARCH_RESULT_COUNT}
                                                    permissions={this.props.permissions}/>
                            </h1>

                            <p>
                                Your search returned no results.&nbsp;
                                <ModalTrigger key="debugQuery" modal={<ShowQueryModal builtQuery={this.props.builtQuery} />}>
                                    <a href="#" onClick={(e) => e.preventDefault()}>Show the Elasticsearch query.</a>
                                </ModalTrigger>
                                <strong>&nbsp;Take a look at the&nbsp;<a
                                    href="https://www.graylog.org/documentation/general/queries/" target="_blank">documentation</a>
                                    &nbsp;if you need help with the search syntax.</strong>
                            </p>
                        </div>
                    </div>
                    <div className="row content">
                        <div className="col-md-12">
                            <div className="support-sources">
                                <h2>Need help?</h2>
                                Do not hesitate to consult the Graylog community if your questions are not answered in the&nbsp;
                                <a href="https://www.graylog.org/documentation/" target="_blank">documentation</a>.

                                <ul>
                                    <li><i className="fa fa-group"></i> <a href="https://www.graylog.org/community-support/" target="_blank">Forum / Mailing list</a></li>
                                    <li><i className="fa fa-github-alt"></i> <a href="https://github.com/Graylog2/graylog2-web-interface/issues" target="_blank">Issue tracker</a></li>
                                    <li><i className="fa fa-heart"></i> <a href="https://www.graylog.com/support/" target="_blank">Commercial support</a></li>
                                </ul>
                            </div>

                        </div>
                    </div>
                </div>);
        }
        return (
            <div id='main-content-search' className='row'>
                <div ref="opa" className="col-md-3" id="sidebar">
                    <div data-spy="affix" data-offset-top="90" style={style} className="hidden-sm hidden-xs" id="sidebar-affix">
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
                                       toggleShouldHighlight={(event) => this.setState({shouldHighlight: !this.state.shouldHighlight})}
                                       currentSavedSearch={SearchStore.savedSearch}
                                       searchInStream={this.props.searchInStream}
                                       permissions={this.props.permissions}
                            />
                    </div>
                </div>
                <div className="col-md-9" id="main-content-sidebar">
                    <FieldStatistics ref='fieldStatisticsComponent'
                                     permissions={this.props.permissions}/>

                    <FieldQuickValues ref='fieldQuickValuesComponent'
                                      permissions={this.props.permissions}/>

                    <LegacyHistogram formattedHistogram={this.props.formattedHistogram}
                                     histogram={this.props.histogram}
                                     permissions={this.props.permissions}/>

                    <FieldGraphs ref='fieldGraphsComponent'
                                 resolution={this.props.histogram['interval']}
                                 from={this.props.histogram['histogram_boundaries'].from}
                                 to={this.props.histogram['histogram_boundaries'].to}
                                 permissions={this.props.permissions}/>

                    <ResultTable messages={this.props.result.messages}
                                 page={this.state.currentPage}
                                 selectedFields={this.state.selectedFields}
                                 sortField={this.state.sortField}
                                 sortOrder={this.state.sortOrder}
                                 resultCount={this.props.result['total_result_count']}
                                 inputs={this.props.inputs}
                                 streams={this.props.streams}
                                 nodes={this.props.nodes}
                                 highlight={this.state.shouldHighlight}
                        />

                </div>
            </div>);
    }
});

module.exports = SearchResult;