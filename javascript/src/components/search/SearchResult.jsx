'use strict';

var $ = require('jquery');

var React = require('react');
var SearchSidebar = require('./SearchSidebar');
var ResultTable = require('./ResultTable');
var LegacyHistogram = require('./LegacyHistogram');
var FieldGraphs = require('./FieldGraphs');
var FieldQuickValues = require('./FieldQuickValues');
var FieldStatistics = require('./FieldStatistics');
var Immutable = require('immutable');

var DashboardStore = require('../../stores/dashboard/DashboardStore');
var SearchStore = require('../../stores/search/SearchStore');

var resizeMutex;

var SearchResult = React.createClass({
    getInitialState() {
        var initialFields = SearchStore.fields;
        return {
            selectedFields: initialFields,
            showAllFields: false,
            currentSidebarWidth: null,
            dashboards: Immutable.Map(),
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
        this._getWritableDashboardList();
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
    _getWritableDashboardList() {
        var promise = DashboardStore.getWritableDashboardList();
        promise.done(dashboards => this.setState({dashboards: Immutable.Map(dashboards)}));
    },

    render() {
        var style = {};
        if (this.state.currentSidebarWidth) {
            style = {width: this.state.currentSidebarWidth};
        }
        var anyHighlightRanges = Immutable.fromJS(this.props.result.messages).some(message => message.get('highlight_ranges') !== null);

        return (
            <div >
                <div ref="opa" className="col-md-3" id="sidebar">
                    <div data-spy="affix" data-offset-top="90" style={style} className="hidden-sm hidden-xs">
                        <SearchSidebar result={this.props.result}
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
                                       dashboards={this.state.dashboards}
                                       searchInStreamId={this.props.searchInStreamId}
                            />
                    </div>
                </div>
                <div className="col-md-9" id="main-content-sidebar">
                    <FieldStatistics ref='fieldStatisticsComponent'
                                     dashboards={this.state.dashboards}/>

                    <FieldQuickValues ref='fieldQuickValuesComponent'
                                      dashboards={this.state.dashboards}/>

                    <LegacyHistogram formattedHistogram={this.props.formattedHistogram}
                                     histogram={this.props.histogram}
                                     dashboards={this.state.dashboards}/>

                    <FieldGraphs ref='fieldGraphsComponent'
                                 resolution={this.props.histogram['interval']}
                                 from={this.props.histogram['histogram_boundaries'].from}
                                 to={this.props.histogram['histogram_boundaries'].to}
                                 dashboards={this.state.dashboards}/>

                    <ResultTable messages={this.props.result.messages}
                                 page={this.state.currentPage}
                                 selectedFields={this.state.selectedFields}
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