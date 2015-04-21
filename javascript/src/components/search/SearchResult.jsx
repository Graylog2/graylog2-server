'use strict';

var React = require('react');
var SearchSidebar = require('./SearchSidebar');
var ResultTable = require('./ResultTable');
var LegacyHistogram= require('./LegacyHistogram');
var Immutable = require('immutable');

var SearchResult = React.createClass({
    getInitialState() {
        return {
            selectedFields: Immutable.Set(['message', 'source'])
        };
    },

    updateSelectedFields(fieldSelection) {
        this.setState({selectedFields: Immutable.Set(fieldSelection)});
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

    /**
     * sort the selected fields alphabetically, but keep timestamp as first field, followed by source if selected.
     * @param fieldSet
     * @returns {*}
     */
    sortFields(fieldSet) {
        var sortedFields = Immutable.OrderedSet();
        if (fieldSet.contains('source')) {
            sortedFields = sortedFields.add('source');
        }
        fieldSet = fieldSet.delete('source');
        var remainingFieldsSorted = fieldSet.sort((a,b) => a.toLowerCase().localeCompare(b.toLowerCase()));
        return sortedFields.concat(remainingFieldsSorted);
    },

    render() {
        return (
            <div >
                <div className="col-md-3" id="sidebar">
                    <SearchSidebar result={this.props.result} selectedFields={this.state.selectedFields} onFieldToggled={this.onFieldToggled}/>
                </div>
                <div className="col-md-9" id="main-content-sidebar">
                    <LegacyHistogram formattedHistogram={this.props.formattedHistogram} histogram={this.props.histogram} />

                    <ResultTable messages={this.props.result.messages} page={this.props.currentPage} selectedFields={this.state.selectedFields} />

                </div>
            </div>);
    }
});

module.exports = SearchResult;