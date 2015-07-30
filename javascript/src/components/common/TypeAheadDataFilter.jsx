'use strict';

var React = require('react');
var ButtonInput = require('react-bootstrap').ButtonInput;
var Immutable = require('immutable');
var TypeAheadInput = require('./TypeAheadInput');

var TypeAheadDataFilter = React.createClass({
    getInitialState() {
        return {
            filterText: '',
            filters: Immutable.OrderedSet(),
            filterByKey: `${this.props.filterBy}s`
        };
    },
    _onSearchTextChanged(event, text) {
        this.setState({filterText: text}, this.filterData);
    },
    _onFilterAdded(event, suggestion) {
        this.setState({
            filters: this.state.filters.add(suggestion[this.props.displayKey]),
            filterText: ''
        }, this.filterData);
        this.refs.typeAheadInput.clear();
    },
    _onFilterRemoved(event) {
        event.preventDefault();
        this.setState({filters: this.state.filters.delete(event.target.getAttribute("data-target"))}, this.filterData);
    },
    _matchFilters(datum) {
        return this.state.filters.every((filter) => {
            return datum[this.state.filterByKey].indexOf(filter) !== -1;
        }, this);
    },
    _matchStringSearch(datum) {
        return this.props.searchInKeys.some((searchInKey) => {
            var key = datum[searchInKey];
            var value = this.state.filterText;

            var containsFilter = function (entry, value) {
                if (typeof entry === 'undefined') {
                    return false;
                }
                return entry.toLocaleLowerCase().indexOf(value.toLocaleLowerCase()) !== -1;
            };

            if (typeof key === 'object') {
                return key.some((arrayEntry) => containsFilter(arrayEntry, value));
            } else {
                return containsFilter(key, value);
            }
        }, this);
    },
    _resetFilters() {
        this.refs.typeAheadInput.clear();
        this.setState({filterText: '', filters: Immutable.OrderedSet()}, this.filterData);
    },
    filterData() {
        if (typeof this.props.filterData === 'function') {
            return this.props.filterData(this.props.data);
        }

        var filteredData = this.props.data.filter((datum) => {
            return this._matchFilters(datum) && this._matchStringSearch(datum);
        }, this);

        this.props.onDataFiltered(filteredData);
    },
    render() {
        var filters = this.state.filters.map((filter) => {
            return (
                <li key={`li-${filter}`}>
                    <span className="pill label label-default">
                        tag: {filter}
                        <a className="tag-remove" data-target={filter} onClick={this._onFilterRemoved}></a>
                    </span>
                </li>
            );
        });

        return (
            <div className="filter">
                <TypeAheadInput ref="typeAheadInput"
                                onFieldChange={this._onSearchTextChanged}
                                onSuggestionSelected={this._onFilterAdded}
                                suggestionText={`Filter by ${this.props.filterBy}: `}
                                suggestions={this.props.filterSuggestions}
                                label={this.props.label}
                                displayKey={this.props.displayKey}>
                    <ButtonInput type="button" value="Reset" style={{marginLeft: 5}} onClick={this._resetFilters}
                                 disabled={this.state.filters.count() === 0 && this.state.filterText === ''}/>
                </TypeAheadInput>
                <ul className="pill-list">
                    {filters}
                </ul>
            </div>
        );
    }
});

module.exports = TypeAheadDataFilter;