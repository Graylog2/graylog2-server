import React from 'react';
import { Button } from 'react-bootstrap';
import Immutable from 'immutable';

import { TypeAheadInput } from 'components/common';

const TypeAheadDataFilter = React.createClass({
  propTypes: {
    data: React.PropTypes.array,
    displayKey: React.PropTypes.string,
    filterBy: React.PropTypes.string,
    filterData: React.PropTypes.func,
    filterSuggestionAccessor: React.PropTypes.string,
    filterSuggestions: React.PropTypes.array,
    label: React.PropTypes.string,
    onDataFiltered: React.PropTypes.func,
    searchInKeys: React.PropTypes.array,
  },
  getInitialState() {
    return {
      filterText: '',
      filters: Immutable.OrderedSet(),
      filterByKey: `${this.props.filterBy}s`,
    };
  },
  _onSearchTextChanged(event) {
    event.preventDefault();
    this.setState({ filterText: this.refs.typeAheadInput.getValue() }, this.filterData);
  },
  _onFilterAdded(event, suggestion) {
    this.setState({
      filters: this.state.filters.add(suggestion[this.props.displayKey]),
      filterText: '',
    }, this.filterData);
    this.refs.typeAheadInput.clear();
  },
  _onFilterRemoved(event) {
    event.preventDefault();
    this.setState({ filters: this.state.filters.delete(event.target.getAttribute('data-target')) }, this.filterData);
  },
  _matchFilters(datum) {
    return this.state.filters.every((filter) => {
      let dataToFilter = datum[this.state.filterByKey];

      if (this.props.filterSuggestionAccessor) {
        dataToFilter = dataToFilter.map(data => data[this.props.filterSuggestionAccessor].toLocaleLowerCase());
      } else {
        dataToFilter = dataToFilter.map(data => data.toLocaleLowerCase());
      }

      return dataToFilter.indexOf(filter.toLocaleLowerCase()) !== -1;
    }, this);
  },
  _matchStringSearch(datum) {
    return this.props.searchInKeys.some((searchInKey) => {
      const key = datum[searchInKey];
      const value = this.state.filterText;

      if (key === null) {
        return false;
      }
      const containsFilter = (entry, thisValue) => {
        if (typeof entry === 'undefined') {
          return false;
        }
        return entry.toLocaleLowerCase().indexOf(thisValue.toLocaleLowerCase()) !== -1;
      };

      if (typeof key === 'object') {
        return key.some(arrayEntry => containsFilter(arrayEntry, value));
      }
      return containsFilter(key, value);
    }, this);
  },
  _resetFilters() {
    this.refs.typeAheadInput.clear();
    this.setState({ filterText: '', filters: Immutable.OrderedSet() }, this.filterData);
  },
  filterData() {
    if (typeof this.props.filterData === 'function') {
      return this.props.filterData(this.props.data);
    }

    const filteredData = this.props.data.filter((datum) => {
      return this._matchFilters(datum) && this._matchStringSearch(datum);
    }, this);

    this.props.onDataFiltered(filteredData);
  },
  render() {
    const filters = this.state.filters.map((filter) => {
      return (
        <li key={`li-${filter}`}>
          <span className="pill label label-default">
            {this.props.filterBy}: {filter}
            <a className="tag-remove" data-target={filter} onClick={this._onFilterRemoved} />
          </span>
        </li>
      );
    });

    let suggestions;

    if (this.props.filterSuggestionAccessor) {
      suggestions = this.props.filterSuggestions.map(filterSuggestion => filterSuggestion[this.props.filterSuggestionAccessor].toLocaleLowerCase());
    } else {
      suggestions = this.props.filterSuggestions.map(filterSuggestion => filterSuggestion.toLocaleLowerCase());
    }

    suggestions.filter(filterSuggestion => !this.state.filters.includes(filterSuggestion));

    return (
      <div className="filter">
        <form className="form-inline" onSubmit={this._onSearchTextChanged} style={{ display: 'inline' }}>
          <TypeAheadInput ref="typeAheadInput"
                          onSuggestionSelected={this._onFilterAdded}
                          suggestionText={`Filter by ${this.props.filterBy}: `}
                          suggestions={suggestions}
                          label={this.props.label}
                          displayKey={this.props.displayKey} />
          <Button type="submit" style={{ marginLeft: 5 }}>Filter</Button>
          <Button type="button" style={{ marginLeft: 5 }} onClick={this._resetFilters}
                  disabled={this.state.filters.count() === 0 && this.state.filterText === ''}>
            Reset
          </Button>
        </form>
        <ul className="pill-list">
          {filters}
        </ul>
      </div>
    );
  },
});

export default TypeAheadDataFilter;
