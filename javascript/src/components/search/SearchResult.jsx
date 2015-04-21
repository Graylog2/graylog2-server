'use strict';

var React = require('react');
var SearchSidebar = require('./SearchSidebar');
var ResultTable = require('./ResultTable');
var LegacyHistogram= require('./LegacyHistogram');
var Immutable = require('immutable');

var SearchResult = React.createClass({
    getInitialState() {
        return {
            selectedFields: Immutable.Set(['message', 'source']),
            showAllFields: false
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
        var remainingFieldsSorted = fieldSet.sort((a,b) => a.toLowerCase().localeCompare(b.toLowerCase()));
        return sortedFields.concat(remainingFieldsSorted);
    },

    render() {
        return (
            <div >
                <div className="col-md-3" id="sidebar">
                    <SearchSidebar result={this.props.result}
                                   selectedFields={this.state.selectedFields}
                                   fields={this._fields()}
                                   showAllFields={this.state.showAllFields}
                                   togglePageFields={this.togglePageFields}
                                   onFieldToggled={this.onFieldToggled}
                                   predefinedFieldSelection={this.predefinedFieldSelection}/>
                </div>
                <div className="col-md-9" id="main-content-sidebar">
                    <LegacyHistogram formattedHistogram={this.props.formattedHistogram} histogram={this.props.histogram} />

                    <ResultTable messages={this.props.result.messages} page={this.props.currentPage} selectedFields={this.state.selectedFields} />

                </div>
            </div>);
    }
});

module.exports = SearchResult;