'use strict';

var $ = require('jquery');

var React = require('react');
var SearchSidebar = require('./SearchSidebar');
var ResultTable = require('./ResultTable');
var LegacyHistogram = require('./LegacyHistogram');
var Immutable = require('immutable');

var resizeMutex;

var SearchResult = React.createClass({
    getInitialState() {
        return {
            selectedFields: Immutable.Set(['message', 'source']),
            showAllFields: false,
            currentSidebarWidth: null
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
        this.setState({selectedFields: this.sortFields(fieldSelection)});
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
        this.setState({selectedFields: this.sortFields(newFieldSet)});
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

    componentDidMount() {
        this._updateWidth();
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
                                       predefinedFieldSelection={this.predefinedFieldSelection}/>
                    </div>
                </div>
                <div className="col-md-9" id="main-content-sidebar">
                    <LegacyHistogram formattedHistogram={this.props.formattedHistogram}
                                     histogram={this.props.histogram}/>

                    <ResultTable messages={this.props.result.messages}
                                 page={this.props.currentPage}
                                 selectedFields={this.state.selectedFields}
                                 resultCount={this.props.result['total_result_count']}
                                 inputs={this.props.inputs}
                                 streams={this.props.streams}
                                 nodes={this.props.nodes}
                        />

                </div>
            </div>);
    }
});

module.exports = SearchResult;